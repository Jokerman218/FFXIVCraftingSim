package skills;

import java.util.Random;

import engine.CraftingStatus;
import engine.Engine;

public enum PQSkill implements Skill
{
	Basic_Synthesis("����", 				 
														 0, 10, 1.0, 0, 0.9),
	Standard_Synthesis("�м�����", 				 
			 											 15, 10, 1.5, 0, 0.9), //
	Careful_Synthesis("ģ������", 			 
														 0, 10, 0.9, 0, 1),
	Careful_Synthesis_II("ģ������II", 			 
			 											 0, 10, 1.2, 0, 1), //
	Careful_Synthesis_III("ģ������III", 			 
			 											 7, 10, 1.5, 0, 1), //
	Rapid_Synthesis("��������", 				 
														 0, 10, 2.5, 0, 0.5),
	Rapid_Synthesis_II("��������II", 				 
			 											 12, 10, 3.0, 0, 0.6),
//	Groundwork("��������", 						
//														18, 20, 3.0, 0, 1.0),
	Focused_Synthesis("ע������", 			 
														 5, 10, 2.0, 0, 0.5),
	Brand_of_the_Elements("Ԫ��֮ӡ��", 	 
														 6, 10, 1.0, 0, 1),
	Intensive_Synthesis("��������",		 
														 6, 10, 3.0, 0, 1),
	
	Basic_Touch("�ӹ�",						 
														18, 10, 0, 1.00, 0.7),
	Standard_Touch("�м��ӹ�",				 
														32, 10, 0, 1.25, 0.8),
	Advanced_Touch("�ϼ��ӹ�",				 
														48, 10, 0, 1.25, 0.9), //
	Hasty_Touch("�ִ�",						  
														 0, 10, 0, 1.00, 0.5),
	Hasty_Touch_II("�ִ�II",						  
														5, 10, 0, 1.00, 0.6), //
	
	Precise_Touch("���мӹ�",					 
														18, 10, 0, 1.50, 0.7),
	Focused_Touch("ע�Ӽӹ�",					 
														18, 10, 0, 1.50, 0.5),
	Patient_Touch("ר�ļӹ�",					 
														 6, 10, 0, 1.00, 0.5),
	Prudent_Touch("��Լ�ӹ�",					 
														21,  5, 0, 1.00, 0.7),
//	Preparatory_Touch("���ϼӹ�",			 
//														40, 20, 0, 2.00, 1.0),
	
//	Delicate_Synthesis("��������",		 
//														32, 10, 1.0, 1.0, 1.0);
	
	;
	String name;
	String imgAddress;
	int cpCost;	
	int durabilityCost; 
	double progressRate;
	double qualityRate;
	double successRate;

	static Random r;
	
	static Engine engine;
	
	static {
		r = new Random();
	}
	
	private PQSkill(String name,  int cpCost, int durabilityCost,
			double progressRate, double qualityRate, double successRate) {
		this.name = name;
		this.imgAddress = "/icons/" + this.toString() + ".png";
		this.cpCost = cpCost;
		this.durabilityCost = durabilityCost;
		this.progressRate = progressRate;
		this.qualityRate = qualityRate;
		this.successRate = successRate;		
	}
	
	@Override
	public String getName() {
		return name;
	}
	
	@Override 
	public String getBaseProgressRate() {
		return Double.toString(progressRate * 100) + "%";
	}
	
	@Override 
	public String getBaseQualityRate() {
		return Double.toString(qualityRate * 100) + "%";
	}
	
	@Override
	public String getAddress() {
		return imgAddress;
	}
	
	@Override
	public double getActualProgressRate()
	{
		if(progressRate == 0) return 0;
		double temp = 1.0;
		boolean hasNOTE = false;
		for(ActiveBuff ab: engine.getActiveBuffs()) {
			if(ab.buff==Buff.name_of_the_elements) {hasNOTE = true;}
			temp += ab.buff.getProgressBuff();
		}
		
		if(this==Brand_of_the_Elements && hasNOTE) {
			temp += (double)engine.getPresentProgress()/engine.getTotalProgress()*2;
		}
		
		return temp * progressRate;
	}

	@Override
	public double getActualQualityRate()
	{
		if(qualityRate == 0) return 0;
		double temp = 1.0;
		for(ActiveBuff ab: engine.getActiveBuffs()) {
			temp += ab.buff.getQualityBuff();
		}
		
		return temp * qualityRate;
	}

	@Override
	public boolean isSuccess()
	{
		if(engine.isObserved() && (this == Focused_Synthesis || this == Focused_Touch)) {return true;}
		return r.nextDouble() <= (successRate + (engine.getCraftingStatus() == CraftingStatus.Centered ? 0.25 : 0));
	}

	@Override
	public int getCPCost()
	{
		return cpCost;
	}

	@Override
	public int getDurCost()
	{
		return durabilityCost;
	}
	
	public static void setEngine(Engine e)
	{
		engine = e;
	}
	
	@Override
	public double getSuccessRate() {
		if(successRate == 1.0) {
			return 1.0;
		}
		double d = successRate + (engine.getCraftingStatus() == CraftingStatus.Centered ? 0.25 : 0);
		d = (double)Math.round(d * 100)/100;
		return d;
	}
 
	@Override
	public double getSuccessRate(CraftingStatus cs) {
		if(successRate == 1.0) {
			return 1.0;
		}
		double d = successRate + (cs == CraftingStatus.Centered ? 0.25 : 0);
		d = (double)Math.round(d * 100)/100;
		return d;
	}
	
	public static void setRandom(Random ra) {
		r = ra;
	}
	
	@Override
	public int getSkillIndex()
	{
		
		for(int i = 0; i < values().length; i++) {
			if(this == values()[i]) {
				return i;
			}
		}
		return -1;
	}
}
