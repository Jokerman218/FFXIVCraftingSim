package skills;

import java.util.Random;

import application.CraftingStatus;
import application.Engine;

public enum PQSkill implements Skill
{
	Basic_Synthesis("����", 				 
														 0, 10, 1.2, 0, 1),
	Careful_Synthesis("ģ������", 			 
														 7, 10, 1.5, 0, 1),
	Rapid_Synthesis("��������", 				 
														 0, 10, 5.0, 0, 0.5),
	Groundwork("��������", 						
														18, 20, 3.0, 0, 1.0),
	Focused_Synthesis("ע������", 			 
														 5, 10, 2.0, 0, 0.5),
	Brand_of_the_Elements("Ԫ��֮ӡ��", 	 
														 6, 10, 1.0, 0, 1),
	Intensive_Synthesis("��������",		 
														 6, 10, 3.0, 0, 1),
	
	Basic_Touch("�ӹ�",						 
														18, 10, 0, 1.00, 1.0),
	Standard_Touch("�м��ӹ�",				 
														32, 10, 0, 1.25, 1.0),
	Hasty_Touch("�ִ�",						  
														 0, 10, 0, 1.00, 0.6),
	Precise_Touch("���мӹ�",					 
														18, 10, 0, 1.50, 1.0),
	Focused_Touch("ע�Ӽӹ�",					 
														18, 10, 0, 1.50, 0.5),
	Patient_Touch("ר�ļӹ�",					 
														 6, 10, 0, 1.00, 0.5),
	Prudent_Touch("��Լ�ӹ�",					 
														25,  5, 0, 1.00, 1.0),
	Preparatory_Touch("���ϼӹ�",			 
														40, 20, 0, 2.00, 1.0),
	
	Delicate_Synthesis("��������",		 
														32, 10, 1.0, 1.0, 1.0);
	
	
	String name;
	String imgAddress;
	int cpCost;	
	int durabilityCost; 
	double progressRate;
	double qualityRate;
	double successRate;

	Random r;
	
	Engine engine;
	
	private PQSkill(String name,  int cpCost, int durabilityCost,
			double progressRate, double qualityRate, double successRate) {
		this.name = name;
		this.imgAddress = "/icons/" + this.toString() + ".png";
		this.cpCost = cpCost;
		this.durabilityCost = durabilityCost;
		this.progressRate = progressRate;
		this.qualityRate = qualityRate;
		this.successRate = successRate;
		
		r = new Random();
		
	}
	
	@Override
	public String getName() {
		return name;
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
	
	@Override
	public void setEngine(Engine e)
	{
		engine = e;
	}

}