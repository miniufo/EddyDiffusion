//
package diffuse;

import miniufo.application.basic.SphericalHarmonicExpansion;
import miniufo.application.statisticsModel.FilterMethods;
import miniufo.application.statisticsModel.StatisticsBasicAnalysisMethods;
import miniufo.descriptor.DataDescriptor;
import miniufo.diagnosis.DiagnosisFactory;
import miniufo.diagnosis.MDate;
import miniufo.diagnosis.Range;
import miniufo.diagnosis.SphericalSpatialModel;
import miniufo.diagnosis.Variable;
import miniufo.io.DataIOFactory;
import miniufo.io.DataRead;
import miniufo.io.DataWrite;
import miniufo.test.application.basic.GlobalDynamicMethodsInSC;


//
public final class AtmosDiffusivity{
	//
	static final int strYear=1987;
	static final int endYear=2011;
	
	static final String path="d:/Data/ERAInterim/AtmosDiffusivity/";
	
	
	//
	public static void main(String[] args){
		//cStreamFunction();
		//cDiffusivityCoefficient();
		cEddies();
	}
	
	static void cEddies(){
		DiagnosisFactory df=DiagnosisFactory.parseFile(path+"Data.ctl");
		DataDescriptor dd=df.getDataDescriptor();
		
		Variable[] vs=df.getVariables(new Range("",dd),"u","v","t");
		
		Variable um=null;//FilterMethods.annualCycleFilterForDaily4Data(vs[0],strYear);
		Variable vm=null;//FilterMethods.annualCycleFilterForDaily4Data(vs[1],strYear);
		Variable Tm=null;//FilterMethods.annualCycleFilterForDaily4Data(vs[2],strYear);
		
		FilterMethods.removeLinearTrend(vs[0]);
		FilterMethods.removeLinearTrend(vs[1]);
		FilterMethods.removeLinearTrend(vs[2]);
		
		DataWrite dw=DataIOFactory.getDataWrite(dd,path+"filteredData.dat");
		dw.writeData(dd,vs);			dw.closeFile();
		
		DataWrite dw2=DataIOFactory.getDataWrite(dd,path+"meanData.dat");
		dw2.writeData(dd,um,vm,Tm);		dw2.closeFile();
	}
	
	static void cDiffusivityCoefficient(){
		DiagnosisFactory df=DiagnosisFactory.parseFile(path+"sf.ctl");
		DataDescriptor dd=df.getDataDescriptor();
		
		Variable v=df.getVariables(new Range("",dd),"sf")[0];
		
		Variable vm=null;//FilterMethods.annualCycleFilterForDaily4Data(v,strYear);
		FilterMethods.removeLinearTrend(v);
		
		Variable varsf=StatisticsBasicAnalysisMethods.cTVariance(v);
		
		DataWrite dw=DataIOFactory.getDataWrite(dd,path+"filteredSF.dat");
		dw.writeData(dd,v);			dw.closeFile();
		
		DataWrite dw2=DataIOFactory.getDataWrite(dd,path+"meanSF.dat");
		dw2.writeData(dd,vm);		dw2.closeFile();
		
		DataWrite dw3=DataIOFactory.getDataWrite(dd,path+"varSF.dat");
		dw3.writeData(dd,varsf);	dw3.closeFile();
	}
	
	static void cStreamFunction(){
		DiagnosisFactory df=DiagnosisFactory.parseFile(path+"Data.ctl");
		DataDescriptor dd=df.getDataDescriptor();
		
		Range r=new Range("t(1,397)",dd);
		
		Variable u=new Variable("u",r);
		Variable v=new Variable("v",r);
		
		DataRead dr=DataIOFactory.getDataRead(dd);
		dr.setPrinting(false);
		
		SphericalSpatialModel ssm=new SphericalSpatialModel(dd);
		SphericalHarmonicExpansion she=new SphericalHarmonicExpansion(ssm);
		GlobalDynamicMethodsInSC gdm=new GlobalDynamicMethodsInSC(ssm);
		
		she.setM(120);
		
		DataWrite dw=DataIOFactory.getDataWrite(dd,path+"sf.dat");
		
		for(int yy=1;yy<=36524;yy+=397){
			int[] trange=r.getTRange();
			trange[0]=yy;
			trange[1]=yy+397-1;
			
			System.out.println("compute for ["+trange[0]+", "+trange[1]+"]");
			
			dr.readData(u,v);
			
			dw.writeData(she.solvePoissonEquation(gdm.c2DVorticity(u,v)));
			
			for(int l=0;l<10;l++) System.gc();
		}
		
		dr.closeFile();	dw.closeFile();
	}
	
	static int[] getTRange(int year){
		int[] re=new int[3];
		
		re[0]=1;
		
		for(int yy=strYear;yy<year;yy++) re[0]+=(MDate.isLeapYear(yy)?366:365)*4;
		
		re[1]=re[0]+(MDate.isLeapYear(year)?366:365)*4-1;
		re[2]=re[1]-re[0]+1;
		
		return re;
	}
}
