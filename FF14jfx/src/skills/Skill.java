package skills;

import exceptions.CraftingException;
import application.Engine;

public interface Skill
{
//	 String name;
//	 String imgAddress;
//	 int cpCost;	
//	 int durabilityCost; 
//	 double progressRate;
//	 double qualityRate;
//	 double successRate;
//	 boolean oneTime;
//	 boolean firstTurn;
//	
//	Engine engine;
	
//	public boolean isBuff;
//	public double progressbuff;
//	public double qualitybuff;
//	public double duraRecover;
	
	public String getName();
	
	public String getAddress();
	
	public double getActualProgressRate();
	
	public double getActualQualityRate() throws CraftingException;
	
	public boolean isSuccess();
	
	public int getCPCost();
	
	public int getDurCost();
	
	public void setEngine(Engine e);
}