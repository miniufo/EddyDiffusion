//
package GDPSCS;

import java.util.List;

import diffuse.DiffusionModel;
import miniufo.application.statisticsModel.WaveletApplication;
import miniufo.descriptor.DataDescriptor;
import miniufo.diagnosis.DiagnosisFactory;
import miniufo.diagnosis.MDate;
import miniufo.diagnosis.Range;
import miniufo.diagnosis.Variable;
import miniufo.io.DataIOFactory;
import miniufo.io.DataWrite;
import miniufo.lagrangian.GDPDrifter;
import miniufo.util.Region2D;


//
public class Wavelet{
	// South China Sea region
	private static final Region2D SCS=new Region2D(98,0,126,27,"SCS region");
	
	private static final boolean writeTraj=false;
	
	private static final String path="d:/Data/GDP/SCS/Wavelet/";
	
	private static final String[] dfiles={
		path+"GDP/buoydata_1_5001.dat",
		path+"GDP/buoydata_5001_10000.dat",
		path+"GDP/buoydata_10001_dec12.dat"
	};
	
	private static final String[] mfiles={
		path+"GDP/dirfl_1_5000.dat",
		path+"GDP/dirfl_5001_10000.dat",
		path+"GDP/dirfl_10001_dec12.dat"
	};
	
	
	/** test*/
	public static void main(String[] args){
		List<GDPDrifter> ls=DiffusionModel.getGDPDriftersWithin(dfiles,mfiles,SCS);
		
		if(writeTraj) DiffusionModel.writeTrajAndGS(ls,path,SCS);
		
		postProcess(ls);
	}
	
	static void postProcess(List<GDPDrifter> ls){
		System.out.println(ls.size()+" records");
		
		for(GDPDrifter p:ls) waveletAnalysis(p);
	}
	
	static void waveletAnalysis(GDPDrifter p){
		String template=
		"dset ^test\n"+
		"title waveleet\n"+
		"undef -9.99e8\n"+
		"xdef  1 linear 0 10\n"+
		"ydef  1 linear 0 10\n"+
		"zdef  1 levels 0 1\n"+
		"tdef "+p.getTCount()+" linear "+new MDate(p.getTime(0)).toGradsDate()+" 6hr\n"+
		"vars 1\n"+
		"test 1 99 test variable\n"+
		"endvars\n";
		
		DiagnosisFactory df=DiagnosisFactory.parseContent(template);
		DataDescriptor dd=df.getDataDescriptor();
		
		Variable u=new Variable("u",false,new Range(p.getTCount(),1,1,1));
		Variable v=new Variable("v",false,new Range(p.getTCount(),1,1,1));
		
		u.setCommentAndUnit("u-current");	u.setUndef(-9999);
		v.setCommentAndUnit("v-current");	v.setUndef(-9999);
		
		float[] udata=u.getData()[0][0][0];
		float[] vdata=v.getData()[0][0][0];
		
		for(int l=0,L=p.getTCount();l<L;l++){
			udata[l]=p.getRecord(l).getDataValue(0);
			vdata[l]=p.getRecord(l).getDataValue(1);
		}
		
		wavelet(u,dd,p.getID());
		wavelet(v,dd,p.getID());
	}
	
	static void wavelet(Variable v,DataDescriptor dd,String id){
		WaveletApplication wa=new WaveletApplication(v);
		
		Variable re=wa.getReal();
		Variable mo=wa.getMod();
		Variable coi=wa.getCOI();
		Variable gws=wa.getGWS();
		Variable csig=wa.getCHISig(0.95f);
		Variable gsig=wa.getGWSSig(0.95f);
		
		DataWrite cdws=DataIOFactory.getDataWrite(dd,path+id+v.getName()+".dat");
		cdws.writeData(coi,re,mo,csig);	cdws.writeCtl(dd,wa.getZDef(),null);	cdws.closeFile();
		
		wa.writeGS(path+id+v.getName()+".gs");
		
		cdws=DataIOFactory.getDataWrite(dd,path+id+"gws"+v.getName()+".dat");
		cdws.writeData(gws,gsig);	cdws.writeCtl(dd,wa.getZDef(),null);	cdws.closeFile();
	}
}
