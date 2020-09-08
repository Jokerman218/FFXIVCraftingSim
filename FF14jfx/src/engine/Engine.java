package engine;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import application.components.LogManager;
import application.components.Timer;
import application.subPane.CraftingHistoryPane;
import exceptions.CraftingException;
import exceptions.ExceptionStatus;
import skills.ActiveBuff;
import skills.Buff;
import skills.BuffSkill;
import skills.PQSkill;
import skills.Skill;
import skills.SpecialSkills;

public class Engine
{	

	
	// Same as those appeared in ViewManager
	private double progressDifference = 0.8;
	private double qualityDifference = 0.6;
	
	private int craftsmanship;
	private int control;
	private int buffControl;
	private int recCraftsmanship; // Recommended craftsmanship
	private int recControl;		  // Recommended control
	private int baseProgEff;	  // Base progress efficiency
	private int baseQltyEff;	  // Base quality efficiency
	private int round;			  // Present round
	private long seed;			  // Random seed
	
	private int coCount;	// Counts the use of Careful_Observation
	
//	private ArrayList<String> logs; // Stores the logs of the whole crafting process
	
	CraftingStatus cs; // Stores the current crafting status (see enum CraftingStatus)
	private CraftingStatus lastCs;
	
	private boolean progIncreased;	// Record if the progress and quality has increased or not
	private boolean qltyIncreased;	// These two are for updating the buff
	
	private AI ai;
	
	private EngineStatus es;		// Record the engine status (see enum EngineStatus)
	
	private Timer timer;
	private LogManager lm;
	
	protected ArrayList<ActiveBuff> activeBuffs; // Stores the buffs that are active now
	
	protected int totalDurability;
	protected int presentDurability;
	protected int totalProgress;
	protected int presentProgress;
	protected int totalQuality;
	protected int presentQuality;
	protected int totalCP;
	protected int presentCP;
	protected int innerQuietLvl;
	
	protected boolean observed;  // Record if last turn used the skill Observe
	protected boolean skillSuccess = true; // Record if the skill is success
	

	
	public Engine(int craftsmanship, int control, int totalCP, int totalDurability, 
				int totalProgress, int totalQuality, int recCraftsmanship, int recControl,
				double porgressDifference, double qualityDifference,  
				long seed, CraftingStatus.Mode m) {
		this.craftsmanship = craftsmanship; 
		this.control = control;
		this.totalCP = totalCP;
		this.totalDurability = totalDurability;
		this.totalProgress = totalProgress;
		this.totalQuality = totalQuality;
		this.recCraftsmanship = recCraftsmanship;
		this.recControl = recControl;
		this.progressDifference = porgressDifference;
		this.qualityDifference = qualityDifference;
		this.seed = seed;
		
		activeBuffs = new ArrayList<>();
//		logs = new ArrayList<>();
		lm = new LogManager();
		
		presentDurability = totalDurability;
		presentProgress = 0;
		presentQuality = 0;
		coCount = 0;
		presentCP = totalCP;
		observed = false;
		es = EngineStatus.Crafting;
		progIncreased = false;
		qltyIncreased = false;
		
		ai = new AI(this);
		
		timer = new Timer();
		timer.startTimer();
		
		round = 0;

		cs = CraftingStatus.Normal;
		
		calcBaseProg();			// Calculate the base progress
		calcBaseQlty();			// Calculate the base efficiency
		
		setEnumEngine();		// Set the engine for all the enums
		setRandom();			// Set the random generator for other classes
		
		CraftingStatus.setMode(m); // set the crafting mode 
		
		lm.setBaseInfo(craftsmanship, control, totalCP, totalProgress, totalQuality, totalDurability, this.seed, m);
	}
	
	/**
	 * Create a new active buff
	 * @param b the buff created
	 * @param last the length of the buff
	 */
	public void addActiveBuff(Buff b, int last) {
		for(ActiveBuff ab: activeBuffs) {
			if(ab.buff==b) {
				ab.setRemaining(last);
				return;
			}
		}
		
		activeBuffs.add(new ActiveBuff(b, last));
	}
	
	/**
	 * set the engines for enums
	 */
	private void setEnumEngine()
	{
		PQSkill.setEngine(this);
		BuffSkill.setEngine(this);
		SpecialSkills.setEngine(this);
	}
	
	/**
	 * set the random seed for different classes that has the random generator
	 */
	public void setRandom() {
		Random r = new Random();
		if(seed != 0) {
			r.setSeed(seed);
		} else {
			seed = r.nextLong();
			r.setSeed(seed);
		}
		PQSkill.setRandom(r);			// The other two skill class doesn't need random
		CraftingStatus.setRandom(r);
	}
	
	/**
	 * check which category the skill is
	 * @param sk the skill being processed
	 * @throws CraftingException see crafting exception enum
	 */
	public void useSkill(Skill sk) throws CraftingException { 
		// Check if CP is enough
		if(presentCP < Math.round((double)sk.getCPCost() / (cs == CraftingStatus.Pliant ? 2 : 1))) {
			throw new CraftingException(ExceptionStatus.No_Enough_CP);
		} 
		
		// Check the category of the skill
		if(sk instanceof PQSkill) {
			usePQSkill((PQSkill)sk);
		} else if (sk instanceof BuffSkill) {
			useBuffSkill((BuffSkill)sk);
		} else if (sk instanceof SpecialSkills) {
			useSpecialSkills((SpecialSkills)sk);
		}
	}
	
	/**
	 * Execute a progress or a quality skill
	 * @param sk the skill being processed
	 * @throws CraftingException see crafting exception enum
	 */
	private void usePQSkill(PQSkill sk) throws CraftingException {
		// Check if the requirement is met
		if(sk == PQSkill.Intensive_Synthesis || sk == PQSkill.Precise_Touch) {
			if(cs != CraftingStatus.HQ) {
				throw new CraftingException(ExceptionStatus.Not_HQ);
			}
		}
		if(sk == PQSkill.Prudent_Touch && buffExist(Buff.waste_not)) {
			throw new CraftingException(ExceptionStatus.Waste_Not_Exist);
		}
		
		beginning(sk);

		// Check if the skill is success
		if(sk.isSuccess()) {
			skillSuccess = true;
			forwardProgress(sk);
		} else {
			if(sk == PQSkill.Patient_Touch) {
				innerQuietLvl =  (innerQuietLvl + 1) / 2;
				innerQuietLvl = (innerQuietLvl == 0 ? 1 : innerQuietLvl);
				setBuffInnerQuiet(innerQuietLvl);
			}
		}
		
		lm.setSkillSuccess(skillSuccess);
		
		observed = false;
		finalizeRound(sk);
	}
	
	/**
	 * Execute a buff skill
	 * @param sk
	 * @throws CraftingException see crafting exception enum
	 */
	private void useBuffSkill(BuffSkill sk) throws CraftingException {
		if(sk == BuffSkill.Final_Appraisal) {
			beginning(sk);
			
			skillSuccess = true;
			presentCP--;
			
			lm.setPresentDurability(presentDurability);
			lm.setDurabilityDecrease(0);
			lm.setPresentCP(presentCP);
			lm.setCPDecrease(1);
			lm.nodeFinish();
			
			sk.createBuff();
//			ch.addToQueue(sk, cs, skillSuccess);
			return;
		}
		
		if(sk == BuffSkill.Inner_Quiet) {
			if(innerQuietLvl > 0) {
				throw new CraftingException(ExceptionStatus.Inner_Quiet_Exists);
			}
		}
		if(sk == BuffSkill.Muscle_Memory || sk == BuffSkill.Reflect) {
			if(round != 0) {
				throw new CraftingException(ExceptionStatus.Not_Turn_One);
			}
		}
		
		beginning(sk);

		if(sk.isSuccess()) {
			skillSuccess = true;
			forwardProgress(sk);
		}
		
		observed = false;
		
		for(ActiveBuff ab: activeBuffs) {
			if(ab.buff == sk.getBuff()) {
				activeBuffs.remove(ab);
				break;
			}
		}

		finalizeRound(sk);
		sk.createBuff(); // Since reduce buff is in finalizeRounds() so I create buff after that 
	}
	
	/**
	 * Execute a special skill 
	 * @param sk
	 * @throws CraftingException see crafting exception enum
	 */
	public void useSpecialSkills(SpecialSkills sk) throws CraftingException {
		if(innerQuietLvl <= 1 && sk == SpecialSkills.Byregots_Blessing) {
			throw new CraftingException(ExceptionStatus.No_Inner_Quiet); 
		}
		if(sk == SpecialSkills.Careful_Observation) {
			if(coCount >= 3) {
				throw new CraftingException(ExceptionStatus.Maximun_Reached);
			}
			coCount++;
			beginning(sk);
			skillSuccess = true;
			
			lm.setPresentDurability(presentDurability);
			lm.setDurabilityDecrease(0);
			lm.setPresentCP(presentCP);
			lm.setCPDecrease(0);
			lm.nodeFinish();
			
//			ch.addToQueue(sk, cs, true);
			lastCs = cs;
			cs = CraftingStatus.getNextStatus();			
			return;
		}
		beginning(sk);

		observed = false;

		if(sk.isSuccess()) {
			skillSuccess = true;
			forwardProgress(sk);
		}
		
		sk.execute();
		finalizeRound(sk);
	}
	
	/**
	 * The works done before the execution of the skill
	 * @param sk 
	 */
	public void beginning(Skill sk) {
		skillSuccess = false;
		progIncreased = false;
		qltyIncreased = false;
		
		lm.init();
		lm.setRound(round);
		lm.setCraftingStatus(cs);
		lm.setSkill(sk);
		lm.setOberved(observed);
		lm.setBuffList(activeBuffs);
	}
	
	/**
	 * Calculate rate, then calculate actual increase and add it to present value
	 * @param sk
	 */
	private void forwardProgress(Skill sk) {
		double tempProgressRate = sk.getActualProgressRate();
		double tempQualityRate = sk.getActualQualityRate();
		double statusBuff = 0;
		
		if(cs == CraftingStatus.HQ) {
			statusBuff = 1.5;
		} else if(cs == CraftingStatus.MQ) {
			statusBuff = 4.0;
		} else if(cs == CraftingStatus.LQ) {
			statusBuff = 0.5;
		} else {
			statusBuff = 1.0;
		}
		
		int tempProgressIncrease = (int)Math.floor(baseProgEff * tempProgressRate);
		int tempQualityIncrease  = (int)Math.floor(Math.floor(baseQltyEff * statusBuff * tempQualityRate));
		
		progIncreased = tempProgressIncrease > 0;
		qltyIncreased = tempQualityIncrease > 0;
		
		presentProgress += tempProgressIncrease;
		presentQuality += tempQualityIncrease;
		
		lm.setPresentProgressIncrease(tempProgressIncrease);
		lm.setPresentProgressRate(tempProgressRate);
		lm.setPresentProgress(presentProgress);
		lm.setPresentQualityIncrease(tempQualityIncrease);
		lm.setPresentQualityRate(tempQualityRate);
		lm.setPresentQuality(presentQuality);
		
		// To avoid overflow
		if(presentProgress > totalProgress) {
			presentProgress = totalProgress;
		}
		if(presentQuality > totalQuality) {
			presentQuality = totalQuality;
		}
		
		// Check if final_appraisal takes effect
		if(presentProgress >= totalProgress && buffExist(Buff.final_appraisal)) {
			presentProgress = totalProgress - 1;
			for(ActiveBuff ab: activeBuffs) {
				if(ab.buff == Buff.final_appraisal) {
					activeBuffs.remove(ab);
					break;
				}
			}
		}
		
		// Dealing with special case (about inner quiet)
		if(sk == PQSkill.Patient_Touch) {
			innerQuietLvl *= 2;
			setBuffInnerQuiet(innerQuietLvl);
		} else if(tempQualityIncrease > 0 && buffExist(Buff.inner_quiet)) {
			innerQuietLvl += 1;
			setBuffInnerQuiet(innerQuietLvl);
			if(sk == PQSkill.Preparatory_Touch || sk == PQSkill.Precise_Touch) {
				innerQuietLvl += 1;
				setBuffInnerQuiet(innerQuietLvl);
			}
		} 
		
	}
	
	private void finalizeRound(Skill sk) throws CraftingException {
//		ch.addToQueue(sk, cs, skillSuccess);
		
		successfulUse(sk);
				
		finishCheck();
		updateBuff();
		updateStatus();
		calcBaseQlty();
	}
	
	/**
	 * Works done after the skill is successfully executed(Durability and CP change)
	 * @param sk
	 */
	private void successfulUse(Skill sk) {
		int durDec = (int)Math.round((double)sk.getDurCost() / 
									  (buffExist(Buff.waste_not) ? 2 : 1) / 
									  (cs == CraftingStatus.Sturdy ? 2 : 1));
		int cpDec = (int)Math.round((double)sk.getCPCost() / (cs == CraftingStatus.Pliant ? 2 : 1));

		presentDurability -= durDec;
		presentCP -= cpDec; 
		round++;
	
		lm.setPresentDurability(presentDurability);
		lm.setDurabilityDecrease(durDec);
		lm.setPresentCP(presentCP);
		lm.setCPDecrease(cpDec);
		lm.nodeFinish();
	}
	
	/**
	 * update the buff (decreasement and clear buff with 0 turn remaining)
	 */
	private void updateBuff() {
		for(int i = 0; i < activeBuffs.size(); i++)
		{
			if(activeBuffs.get(i).buff == Buff.manipulation) {
				presentDurability += 5;
				if(presentDurability > totalDurability) {
					presentDurability = totalDurability;
				}
			}
			activeBuffs.get(i).decrease();
			// clear buff with 0 turns and clear buffs that take effect only once
			if(activeBuffs.get(i).getRemaining() == 0) {
				activeBuffs.remove(i);
				i--;
			} else if(activeBuffs.get(i).buff.isOnce() && (activeBuffs.get(i).buff.getProgressBuff() > 0) && progIncreased)
			{
				activeBuffs.remove(i);
				i--;
			} else if(activeBuffs.get(i).buff.isOnce() && activeBuffs.get(i).buff.getQualityBuff() > 0 && qltyIncreased)
			{
				activeBuffs.remove(i);
				i--;
			}
		}
	}
	
	/**
	 * Check if the craft is finished
	 * @throws CraftingException
	 */
	private void finishCheck() throws CraftingException {
		if(presentProgress >= totalProgress) {
			timer.stopTimer();
			throw new CraftingException(ExceptionStatus.Craft_Success);
		} else if(presentDurability <= 0) {
			timer.stopTimer();
			throw new CraftingException(ExceptionStatus.Craft_Failed);
		} else {
			return; 
		}
	}
	
	/**
	 * Refresh the crafting status
	 */
	private void updateStatus() {
		lastCs = cs;
		cs = CraftingStatus.getNextStatus();
	}
	
	boolean buffExist(Buff b) {
		for(ActiveBuff ab: activeBuffs) {
			if(ab.buff == b) return true;
		}
		return false;
	}
	
	/**
	 * Calculate the base progress, formula is from NGA
	 */
	private void calcBaseProg() {
		baseProgEff = (int)Math.floor(progressDifference * (0.21 * (double)craftsmanship + 2) * 
				(10000.0 + (double)craftsmanship)/(10000.0 + (double)recCraftsmanship));
	}
	
	/**
	 * Calculate the base quality, formula is from NGA
	 */
	private void calcBaseQlty() {
		buffControl = control;
		if(innerQuietLvl > 1) {
			buffControl = (int)Math.floor((double)control * (1.0 + 0.2 * (double)(innerQuietLvl - 1)));
		}
		baseQltyEff = (int)Math.floor(qualityDifference * (0.35 * (double)buffControl + 35) * 
				(10000.0 + (double)buffControl)/(10000.0 + (double)recControl));
	}
	
	/**
	 * Calculate SP after finishing crafting
	 * @return
	 */
	public int SPCalc() {
		final int lv1 = 5800;
		final int lv2 = 6500;
		final int lv3 = 7700;
		
		if(presentQuality/10 < lv1) {
			return 0;
		} else if (presentQuality/10 < lv2) {
			return (int)Math.floor(175 + ((double)presentQuality/10 - lv1) * 0.1);
		} else if (presentQuality/10 < lv3) {
			return (int)Math.floor(370 + ((double)presentQuality/10 - lv2) * 0.45);
		} else {
			return (int)Math.floor(1100 + ((double)presentQuality/10 - lv3) * 0.3);
		}
	}
	
	public double getProgressBuffRate()
	{

		double temp = 1.0;
		for(ActiveBuff ab: activeBuffs) {
			temp += ab.buff.getProgressBuff();
		}
		
		return temp;
	}

	public double getQualityBuffRate()
	{
		double temp = 1.0;
		for(ActiveBuff ab: activeBuffs) {
			temp += ab.buff.getQualityBuff();
		}
		
		return temp;
	}
	
	/**
	 * just.... adds to logs
	 */
//	public void addToLogs(String s) {
//		logs.add(s);
//	}
	
	/**
	 * Since the inner quiet is abnormal, so I set it separately, also
	 * it wont be decreased during the buff decreasement progress (see buff enum)
	 * @param val
	 */
	public void setBuffInnerQuiet(int val) {
		if(innerQuietLvl >= 11) {
			innerQuietLvl = 11;
		}
		for(ActiveBuff ab: activeBuffs) {
			if(ab.buff == Buff.inner_quiet) {
				ab.setRemaining(innerQuietLvl);
				return;
			}
		}
	}
	
	/**
	 * Get the recommended skill based on present status
	 * @return the skill recommended
	 */
	public Skill getRecSkill() {
		return ai.RecSkill();
	}

	
	// == getters and setters ==
	public List<ActiveBuff> getActiveBuffs() {
		return activeBuffs;
	}

	public int getTotalDurability()
	{
		return totalDurability;
	}
	
	public int getPresentDurability()
	{
		return presentDurability;
	}
	
	public void setPresentDurability(int i) {
		presentDurability = i;
	}

	public int getTotalProgress()
	{
		return totalProgress;
	}

	public int getPresentProgress()
	{
		return presentProgress;
	}

	public int getTotalQuality()
	{
		return totalQuality;
	}

	public int getPresentQuality()
	{
		return presentQuality;
	}

	public int getTotalCP()
	{
		return totalCP;
	}

	public int getPresentCP()
	{
		return presentCP;
	}
	
	public void setPresentCP(int i) {
		presentCP = i;
	}
	
	public boolean isObserved() {
		return observed;
	}
	
	public void setObserved(boolean b) {
		observed = b;
	}
	
	public int getBaseProgEff() {
		return (int)((double)baseProgEff * getProgressBuffRate());
	}
	
	public int getBaseQltyEff() {
		return (int)((double)baseQltyEff * getQualityBuffRate());
	}
	
	public int getInnerQuiet() {
		return innerQuietLvl;
	}
	
	public void setInnerQuiet(int i) {
		innerQuietLvl = i;
	}
	
	public CraftingStatus getCraftingStatus() {
		return cs;
	}
	
	public EngineStatus getEngineStatus() {
		return es;
	}
	
	public void setEngineStatus(EngineStatus es) {
		this.es = es;
	}
	
	public int getRound() {
		return round + 1;
	}
	
//	public ArrayList<String> getLogs() {
//		return new ArrayList<String>(logs);
//	}
	
	public double getRuntime() {
		return timer.getTime();
	}

	public int getCraftsmanship()
	{
		return craftsmanship;
	}

	public int getControl()
	{
		return control;
	}
	
	public void setCraftingStatus(CraftingStatus cs) {
		this.cs = cs;
	}
	
	public boolean isSkillSuccess() {
		return skillSuccess;
	}
	
	public CraftingStatus getLastCraftingStatus() {
		return lastCs;
	}
	
	public long getSeed() {
		return seed;
	}
	
	public LogManager getLogManager() {
		return lm;
	}
	
	public int getFinalizeSequence() {
		return ai.finalizeSequence;
	}
}
