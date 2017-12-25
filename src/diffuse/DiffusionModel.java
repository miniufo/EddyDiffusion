package diffuse;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;
import miniufo.application.advanced.CoordinateTransformation;
import miniufo.application.statisticsModel.BinningStatistics;
import miniufo.basic.ArrayUtil;
import miniufo.basic.InterpolationModel;
import miniufo.basic.InterpolationModel.Type;
import miniufo.database.AccessGDPDrifter;
import miniufo.descriptor.DataDescriptor;
import miniufo.diagnosis.DiagnosisFactory;
import miniufo.diagnosis.MDate;
import miniufo.diagnosis.Range;
import miniufo.diagnosis.SpatialModel;
import miniufo.diagnosis.Variable;
import miniufo.lagrangian.GDPDrifter;
import miniufo.lagrangian.LagrangianUtil;
import miniufo.lagrangian.MetaData;
import miniufo.lagrangian.Particle;
import miniufo.lagrangian.Record;
import miniufo.util.GridDataFetcher;
import miniufo.util.Region2D;


//
public final class DiffusionModel{
	//
	public enum Method{Bin,Season2,Season4,GM,GM2,GM3,True}
	
	public static final int[][] season4=new int[][]{{1,2,3},{4,5,6},{7,8,9},{10,11,12}};
	public static final int[][] season2=new int[][]{{1,2,3,4,5,6},{7,8,9,10,11,12}};
	
	
	/**
	 * get drifter data within a region
	 * 
	 * @param	files		file paths
	 * @param	dataLen		initial length of attached data
	 * @param	lon1		start longitude
	 * @param	lat1		start latitude
	 * @param	lon2		end longitude
	 * @param	lat2		end latitude
	 */
	public static List<GDPDrifter> getGDPDriftersWithin(String[] files,String[] meta,int dataLen,Region2D region){
		int fcount=files.length;
		
		if(fcount!=meta.length)
		throw new IllegalArgumentException("drifter files should be accompanied by meta files");
		
		List<GDPDrifter> res=new ArrayList<>();
		
		for(int f=0;f<fcount;f++){
			List<GDPDrifter> all=new ArrayList<>();
			List<MetaData> metas=new ArrayList<>();
			
			AccessGDPDrifter.parseBasicGDPInfo(all,files[f],dataLen,region);
			AccessGDPDrifter.parseGDPMetaData(metas,meta[f]);
			AccessGDPDrifter.attachDrogueData(all,metas);
			
			System.out.println(
				"\nsize: "+all.size()+
				",\tlength: "+LagrangianUtil.cTotalDrifterYear(all)+
				" drifter-years\n"
			);
			
			processInvalidDrifter(all);
			
			for(GDPDrifter drftr:all){
				if(drftr.isContinuous()){
					drftr.removeEndpointUndefVelocityRecords();
					
					if(drftr.getTCount()==0) continue;
					
					GDPDrifter[] drs=null;
					if(drftr.hasUndefRecords(0)){
						System.out.println(drftr.getID()+" has undef velocity record");
						drs=drftr.splitByUndef();
						
					}else drs=new GDPDrifter[]{drftr};
					
					for(GDPDrifter dr:drs)
					if(dr.getTCount()!=0){
						GDPDrifter[] drSegs=AccessGDPDrifter.getRecordsWithinRegion(dr,region);
						
						for(GDPDrifter drSeg:drSegs)
						if(drSeg.getTCount()!=0) res.add(drSeg);
						
					}else System.out.println(dr.getID()+" has no records");
					
				}else throw new IllegalArgumentException("GDPDrifter ("+drftr.getID()+") is not continuous");
			}
			
			all.clear();
			all=null;
			for(int i=0;i<10;i++) System.gc();
		}
		
		return res;
	}
	
	public static List<GDPDrifter> getGDPDriftersWithin(String[] files,String[] meta,Region2D region){
		return getGDPDriftersWithin(files,meta,4,region);
	}
	
	
	/**
	 * get the whole drifter data if one snapshot is within the region
	 * 
	 * @param	files		file paths
	 * @param	dataLen		initial length of attached data
	 * @param	lon1		start longitude
	 * @param	lat1		start latitude
	 * @param	lon2		end longitude
	 * @param	lat2		end latitude
	 */
	public static List<GDPDrifter> getGDPDrifters(String[] files,String[] meta,int dataLen,Region2D region){
		int fcount=files.length;
		
		if(fcount!=meta.length)
		throw new IllegalArgumentException("drifter files should be accompanied by meta files");
		
		List<GDPDrifter> res=new ArrayList<>();
		
		for(int f=0;f<fcount;f++){
			List<GDPDrifter> all=new ArrayList<>();
			List<MetaData> metas=new ArrayList<>();
			
			AccessGDPDrifter.parseBasicGDPInfo(all,files[f],dataLen,region);
			AccessGDPDrifter.parseGDPMetaData(metas,meta[f]);
			AccessGDPDrifter.attachDrogueData(all,metas);
			
			System.out.println(
				"\nsize: "+all.size()+
				",\tlength: "+LagrangianUtil.cTotalDrifterYear(all)+
				" drifter-years\n"
			);
			
			processInvalidDrifter(all);
			
			for(GDPDrifter drftr:all){
				if(drftr.isContinuous()){
					drftr.removeEndpointUndefVelocityRecords();
					
					if(drftr.getTCount()==0) continue;
					
					GDPDrifter[] drs=null;
					
					if(drftr.hasUndefRecords(0)){
						System.out.println(drftr.getID()+" has undef velocity record");
						drs=drftr.splitByUndef();
						
					}else drs=new GDPDrifter[]{drftr};
					
					for(GDPDrifter dr:drs)
					if(dr.getTCount()!=0)res.add(dr);
					else System.out.println(dr.getID()+" has no records");
					
				}else throw new IllegalArgumentException("GDPDrifter ("+drftr.getID()+") is not continuous");
			}
			
			all.clear();
			all=null;
			for(int i=0;i<10;i++) System.gc();
		}
		
		return res;
	}
	
	public static List<GDPDrifter> getGDPDrifters(String[] files,String[] meta,Region2D region){
		return getGDPDrifters(files,meta,4,region);
	}
	
	
	/**
	 * split drifter data into segments of the length seglen
	 * 
	 * @param	list		a list of drifter data
	 * @param	seglen		segment length
	 */
	public static List<GDPDrifter> getGDPDrifterSegments(List<GDPDrifter> list,int seglen){
		if(seglen<1) throw new IllegalArgumentException("segment length is smaller than 1");
		
		List<GDPDrifter> segl=new ArrayList<GDPDrifter>();
		
		for(GDPDrifter dr:list){
			GDPDrifter[] segs=dr.split(seglen);
			
			for(GDPDrifter seg:segs) if(seg.getTCount()==seglen)
			segl.add(seg);
		}
		
		return segl;
	}
	
	
	/**
	 * split drifter data into drogued and undrogued drifter segments
	 * and return the drogued ones
	 * 
	 * @param	list		a list of drifter data
	 */
	public static List<GDPDrifter> getDroguedDrifters(List<GDPDrifter> list){
		List<GDPDrifter> drogued=new ArrayList<>();
		
		for(GDPDrifter dr:list){
			GDPDrifter[] segs=dr.splitByDrogueOffDate(3);
			
			if(segs[0]!=null) drogued.add(segs[0]);
		}
		
		return drogued;
	}
	
	
	/**
	 * interpolate the monthly series to 6hr resolution
	 * 
	 * @param	len		length of the series after interpolation
	 * @param	t		type of interpolation
	 * @param	v		monthly variable
	 */
	public static Variable monthlyToDaily4(int len,Type t,Variable v){
		int z=v.getZCount(),y=v.getYCount(),x=v.getXCount();
		
		Variable r=new Variable(v.getName()+"ia",false,new Range(len,v.getZCount(),y,x));
		r.setCommentAndUnit("interannual variation");
		r.setUndef(Record.undef);
		
		float[] buf=new float[len];
		
		for(int k=0;k<z;k++){
			float[][][] vdata=v.getData()[k];
			float[][][] rdata=r.getData()[k];
			
			for(int j=0;j<y;j++)
			for(int i=0;i<x;i++){
				monthlySeriesToDaily4(vdata[j][i],buf,t);
				System.arraycopy(buf,0,rdata[j][i],0,len);
			}
		}
		
		return r;
	}
	
	
	/**
	 * remove drifter data if it is within a region at a specific time
	 * 
	 * @param	list		a list of drifter data
	 * @param	lon1		start longitude
	 * @param	lat1		start latitude
	 * @param	lon2		end longitude
	 * @param	lat2		end latitude
	 */
	public static void removeDrifterWithin(List<? extends Particle> ls,Region2D region){
		List<Particle> remove=new ArrayList<>();
		
		for(Particle dr:ls)
		for(int l=0,L=dr.getTCount();l<L;l++){
			Record r=dr.getRecord(l);
			
			float lon=r.getXPos();
			float lat=r.getYPos();
			
			if(region.inRange(lon,lat)){
				remove.add(dr);
				break;
			}
		}
		
		ls.removeAll(remove);
	}
	
	/**
	 * remove a specified signal represented monthly by vname in ctl file
	 * 
	 * @param	list		a list of drifter data
	 * @param	idx			index of the AttachedData
	 * @param	template	template for the binning
	 * @param	ctl			ctl file
	 * @param	vname		variable name in the ctl
	 */
	public static void removeSignalFromMonthlyData(List<? extends Particle> ls,int idx,String ctl,String vname){
		DiagnosisFactory df=DiagnosisFactory.parseFile(ctl);
		DataDescriptor grid=df.getDataDescriptor();
		
		Variable v=df.getVariables(new Range("",grid),false,vname)[0];
		
		int y=v.getYCount(),x=v.getXCount();
		
		long[] times=cTimes(
			grid.getTDef().getSamples()[0],
			"6hr",
			new MDate(grid.getTDef().getSamples()[grid.getTCount()-1].getYear(),12,31,18,0,0)
		);
		
		float  undef=grid.getUndef(vname);
		float[] xdef=grid.getXDef().getSamples();
		float[] ydef=grid.getYDef().getSamples();
		float[][][] vdata=v.getData()[0];
		
		Record[][][] rs=binningRecords(ls,grid);
		
		for(int j=0;j<y;j++)
		for(int i=0;i<x;i++)
		if(rs[j][i].length!=0){
			if(vdata[j][i][0]!=undef){
				float[] daily4=new float[times.length];
				monthlySeriesToDaily4(vdata[j][i],daily4,Type.LINEAR);
				
				for(Record r:rs[j][i])
				for(int l=0,L=times.length;l<L;l++)
				if(r.getTime()==times[l]){
					float ov=r.getDataValue(idx);
					if(ov!=Record.undef) r.setData(idx,ov-daily4[l]);
					break;
				}
			}
			else System.out.println("bins ["+xdef[i]+"E, "+ydef[j]+"N] has drifters with undefined gridded data");
		}
	}
	
	
	/**
	 * bilinearly interpolated gridded data onto drifter's time and position
	 * 
	 * @param	list	a list of drifter data
	 * @param	dd		a data descriptor
	 * @param	vnames	names of variables to be interpolated
	 */
	public static void addGridDataToDrifter(List<? extends Particle> ls,DataDescriptor dd,String... vnames){
		if(vnames==null||vnames.length==0)
		throw new IllegalArgumentException("no valid variable name");
		
		System.out.print("\nstart adding gridded data");
		
		int vlen=vnames.length;
		float[] undefs=new float[vlen];
		GridDataFetcher[] gds=new GridDataFetcher[vlen];
		
		for(int m=0;m<vlen;m++){
			undefs[m]=dd.getUndef(vnames[m]);
			gds[m]=new GridDataFetcher(dd);
		}
		
		for(int l=0,L=dd.getTCount();l<L;l++){
			long time=dd.getTDef().getSamples()[l].getLongTime();
			
			Variable[] bufs=new Variable[vlen];
			
			for(int m=0;m<vlen;m++) bufs[m]=gds[m].prepareXYBuffer(vnames[m],l+1,1);
			
			for(Particle dftr:ls){
				for(int ll=0,LL=dftr.getTCount();ll<LL;ll++){
					Record r=dftr.getRecord(ll);
					
					if(r.getTime()==time){
						float lon=r.getXPos();
						float lat=r.getYPos();
						
						for(int m=0;m<vlen;m++){
							float data=gds[m].fetchXYBuffer(lon,lat,bufs[m]);
							r.setData(m+4,data==undefs[m]?Record.undef:data);
						}
					}
				}
				
				dftr.setAttachedDataNames("uvel","vvel","temp","drogueState","uwnd","vwnd");
			}
			
			if(l%(365*4)==0) System.out.print(".");
		}
		
		System.out.println("\n");
	}
	
	/**
	 * mapping the wind data to Ekman current
	 * Reference: Niiler et al. 2003, GRL
	 * 
	 * @param	list	a list of drifter data
	 */
	public static void mappingEkmanCurrent(List<? extends Particle> ls){
		for(Particle dr:ls){
			for(int l=0,L=dr.getTCount();l<L;l++){
				Record r=dr.getRecord(l);
				
				float uwind=r.getDataValue(4);
				float vwind=r.getDataValue(5);
				
				float[] ek=cEkmanCurrent(uwind,vwind,r.getYPos());
				
				r.setData(6,ek[0]);
				r.setData(7,ek[1]);
			}
			
			dr.setAttachedDataNames(ArrayUtil.concatAll(String.class,dr.getDataNames(),"Uek","Vek"));
		}
	}
	
	/**
	 * correct wind-induced slippage of drifter data
	 * 
	 * @param	list	a list of drifter data
	 * @param	dd		DataDescriptor of wind data
	 * @param	u		u-wind name in DataDescriptor
	 * @param	v		v-wind name in DataDescriptor
	 */
	public static void correctWindSlip(List<? extends Particle> ls,DataDescriptor dd,String u,String v){
		float CoeffDrog=7e-4f,CoeffUndr=1.64e-2f;
		
		System.out.println("\nstart correcting wind-slip drifter data" +
			"using "+CoeffDrog+" and "+CoeffUndr+" for drogued and undrogued drifter respectively..."
		);
		
		GridDataFetcher gdsU=new GridDataFetcher(dd);
		GridDataFetcher gdsV=new GridDataFetcher(dd);
		
		for(int l=0,L=dd.getTCount();l<L;l++){
			long time=dd.getTDef().getSamples()[l].getLongTime();
			
			Variable bufU=gdsU.prepareXYBuffer(u,l+1,1);
			Variable bufV=gdsV.prepareXYBuffer(v,l+1,1);
			
			for(Particle dftr:ls){
				for(int ll=0,LL=dftr.getTCount();ll<LL;ll++){
					Record r=dftr.getRecord(ll);
					
					if(r.getTime()==time){
						float lon=r.getXPos();
						float lat=r.getYPos();
						
						float uwind=gdsU.fetchXYBuffer(lon,lat,bufU);
						float vwind=gdsV.fetchXYBuffer(lon,lat,bufV);
						
						float ucurr=r.getDataValue(0);	// u current
						float vcurr=r.getDataValue(1);	// v current
						float drgSt=r.getDataValue(3);	// drogue state
						
						float[] slip=null;
						
						if(drgSt==1) slip=cWindSlip(uwind,vwind,CoeffDrog);
						else if(drgSt==-1) slip=cWindSlip(uwind,vwind,CoeffUndr);
						else throw new IllegalArgumentException(
							"unknown drogue state ("+drgSt+"), 1 for drogued and -1 for undrogued"
						);
						
						r.setData(0,ucurr-slip[0]);
						r.setData(1,vcurr-slip[1]);
					}
				}
				
				dftr.setAttachedDataNames("uvel","vvel","temp","drogueState");
			}
			
			if(l%(365*4)==0) System.out.print(".");
		}
		
		System.out.println("\n");
	}
	
	public static void correctWindSlip(List<? extends Particle> ls){
		float CoeffDrog=7e-4f,CoeffUndr=1.64e-2f;
		
		System.out.println("\nstart correcting wind-slip drifter data" +
			"using "+CoeffDrog+" and "+CoeffUndr+" for drogued and undrogued drifter respectively..."
		);
		
		for(Particle dftr:ls){
			for(int ll=0,LL=dftr.getTCount();ll<LL;ll++){
				Record r=dftr.getRecord(ll);
				
				float ucurr=r.getDataValue(0);	// u current
				float vcurr=r.getDataValue(1);	// v current
				float drgSt=r.getDataValue(3);	// drogue state
				float uwind=r.getDataValue(4);	// u wind
				float vwind=r.getDataValue(5);	// v wind
				
				float[] slip=null;
				
				if(drgSt==1) slip=cWindSlip(uwind,vwind,CoeffDrog);
				else if(drgSt==-1) slip=cWindSlip(uwind,vwind,CoeffUndr);
				else throw new IllegalArgumentException(
					"unknown drogue state ("+drgSt+"), 1 for drogued and -1 for undrogued"
				);
				
				r.setData(0,ucurr-slip[0]);
				r.setData(1,vcurr-slip[1]);
			}
			
			dftr.setAttachedDataNames("uvel","vvel","temp","drogueState","uwind","vwind");
		}
		
		System.out.println("finished\n");
	}
	
	
	/**
	 * project the current in along- and cross-stream components
	 * 
	 * @param	dd		DataDescriptor
	 * @param	list	a list of drifter data
	 */
	public static void projectCurrentInAlongAndCrossStream(DataDescriptor dd,List<? extends Particle> list){
		Variable[] mcurrent=new BinningStatistics(dd).binningData(list,0,1);
		
		float[][] udata=mcurrent[0].getData()[0][0];
		float[][] vdata=mcurrent[1].getData()[0][0];
		
		for(Particle dr:list){
			for(int l=0,L=dr.getTCount();l<L;l++){
				Record r=dr.getRecord(l);
				
				int itag=dd.getXNum(r.getXPos());
				int jtag=dd.getYNum(r.getYPos());
				
				float u=r.getDataValue(0);
				float v=r.getDataValue(1);
				
				float um=udata[jtag][itag];
				float vm=vdata[jtag][itag];
				
				if(um!=Record.undef&&vm!=Record.undef){
					float[] ac=CoordinateTransformation.projectToNaturalCoords(u,v,um,vm);
					
					r.setData(3,ac[0]);
					r.setData(4,ac[1]);
					
				}else{
					r.setData(3,Record.undef);
					r.setData(4,Record.undef);
				}
			}
			
			dr.setAttachedDataNames(ArrayUtil.concatAll(String.class,dr.getDataNames(),"als","acs"));
		}
	}
	
	
	/**
	 * mapping Ekman current from surface wind
	 * Reference: Niiler et al. 2003, GRL
	 * 
	 * @param	uwind		zonal wind speed at the surface (m/s)
	 * @param	vwind		meridional wind speed at the surface (m/s)
	 * @param	lat			latitude where wind locates
	 * 
	 * @return	current		Ekman current (cm/s)
	 */
	public static float[] cEkmanCurrent(float uwind,float vwind,float lat){
		if(lat<10&&lat>-10)
		return new float[]{Record.undef,Record.undef};
		
		final float A=7e-7f;
		final double theta=Math.toRadians(54);
		
		float f1=2*SpatialModel.EARTH_ROTATE_SPEED*(float)Math.sin(Math.toRadians(lat));
		double the=lat<0?theta:-theta;
		float coef=A/(float)Math.sqrt(Math.abs(f1));
		float uek=coef*(float)(Math.cos(the)*uwind-Math.sin(the)*vwind);
		float vek=coef*(float)(Math.cos(the)*vwind+Math.sin(the)*uwind);
		
		return new float[]{uek,vek};
	}
	
	/**
	 * mapping wind-induced slippage from surface wind using an specified coefficient
	 * 
	 * @param	uwind		zonal wind speed at the surface (m/s)
	 * @param	vwind		meridional wind speed at the surface (m/s)
	 * @param	coeff		coefficient of wind slip
	 * 
	 * @return	slipage 	in cm/s
	 */
	public static float[] cWindSlip(float uwind,float vwind,float coeff){
		return new float[]{coeff*uwind,coeff*vwind};
	}
	
	
	/**
	 * read a list of particles from file
	 * 
	 * @param	fname	file name
	 * 
	 * @return	ps		particles
	 */
	@SuppressWarnings("unchecked")
	public static List<GDPDrifter> readDrifterList(String fname){
		List<GDPDrifter> ps=null;
		
		try(ObjectInputStream os=new ObjectInputStream(new FileInputStream(fname))){
			ps=(List<GDPDrifter>)os.readObject();
			
		}catch(Exception e){ e.printStackTrace(); System.exit(0);}
		
		return ps;
	}
	
	@SuppressWarnings("unchecked")
	public static List<Particle> readParticleList(String fname){
		List<Particle> ps=null;
		
		try(ObjectInputStream os=new ObjectInputStream(new FileInputStream(fname))){
			ps=(List<Particle>)os.readObject();
			
		}catch(Exception e){ e.printStackTrace(); System.exit(0);}
		
		return ps;
	}
	
	/**
	 * write a list of particles to file
	 * 
	 * @param	fname	file name
	 * @param	ps		particles
	 */
	public static void writeDrifterList(String fname,List<GDPDrifter> ps){
		try(ObjectOutputStream os=new ObjectOutputStream(new FileOutputStream(fname))){
			os.writeObject(ps);
			
		}catch(Exception e){ e.printStackTrace(); System.exit(0);}
	}
	
	public static void writeParticleList(String fname,List<Particle> ps){
		try(ObjectOutputStream os=new ObjectOutputStream(new FileOutputStream(fname))){
			os.writeObject(ps);
			
		}catch(Exception e){ e.printStackTrace(); System.exit(0);}
	}
	
	/**
	 * output trajectories if the start point is in a given region,
	 * as well as a gs for plotting all drifters' trajectories
	 * 
	 * @param	ls		a list of drifter data
	 * @param	path	folder for output
	 * @param	lon1	start longitude
	 * @param	lat1	start latitude
	 * @param	lon2	end longitude
	 * @param	lat2	end latitude
	 */
	public static void writeTrajAndGS(List<? extends Particle> ls,String path,Region2D r){
		StringBuffer sb=new StringBuffer();
		sb.append("'sdfopen d:/Data/NCEP/OriginalNC/lsmask.192.94.nc'\n");
		sb.append("'enable print "+path+"trajectory.gmf'\n\n");
		sb.append("'set grid off'\n");
		sb.append("'set grads off'\n");
		sb.append("'set lon "+r.getXMin()+" "+r.getXMax()+"'\n");
		sb.append("'set lat "+r.getYMin()+" "+r.getYMax()+"'\n");
		sb.append("'set mpdset mres'\n\n");
		sb.append("'setvpage 1 1.3 1 1'\n");
		sb.append("'setlopts 7 0.18 5 5'\n");
		sb.append("'set line 2 1 0.1'\n");
		sb.append("'set cmin 99999'\n");
		sb.append("'d lsmask'\n\n");
		
		for(Particle p:ls){
			float lon=p.getXPosition(0);
			float lat=p.getYPosition(0);
			
			if(r.inRange(lon,lat)){
				p.toTrajectoryFile(path);
				sb.append("'tctrack "+path+p.getID()+".txt'\n");
			}
		}
		
		sb.append("\n'draw title GDP drifter trajectories'\n\n");
		sb.append("\n'basemap L 15 1 M'\n\n");
		sb.append("'print'\n");
		sb.append("'c'\n\n");
		sb.append("'disable print'\n");
		sb.append("'close 1'\n");
		sb.append("'reinit'\n");
		
		try(FileWriter fw=new FileWriter(path+"trajectory.gs")){
			fw.write(sb.toString());
			
		}catch(IOException e){ e.printStackTrace(); System.exit(0);}
	}
	
	
	/*** helper methods ***/
	private static void monthlySeriesToDaily4(float[] monthlyData,float[] daily4,Type t){
		int len=daily4.length;
		float[] buffer=new float[len-(14+16)*4];
		
		InterpolationModel.interp1D(monthlyData,buffer,t);
		
		for(int l=0,L=14*4;l<L  ;l++) daily4[l]=monthlyData[0];
		for(int l=len-16*4;l<len;l++) daily4[l]=monthlyData[monthlyData.length-1];
		
		System.arraycopy(buffer,0,daily4,14*4,buffer.length);
	}
	
	private static Record[][][] binningRecords(List<? extends Particle> list,DataDescriptor dd){
		int x=dd.getXCount(),y=dd.getYCount();
		
		  int[][] ptrs =new int[y][x];
		float[][] cdata=new float[y][x];
		Record[][][] rs=new Record[y][x][];
		
		for(Particle dr:list)
		for(int l=0,L=dr.getTCount();l<L;l++){
			Record r=dr.getRecord(l);
			
			int itag=dd.getXNum(r.getXPos());
			int jtag=dd.getYNum(r.getYPos());
			
			cdata[jtag][itag]++;
		}
		
		for(int j=0;j<y;j++)
		for(int i=0;i<x;i++){
			int len=Math.round(cdata[j][i]);
			rs[j][i]=new Record[len];
		}
		
		for(Particle dr:list)
		for(int l=0,L=dr.getTCount();l<L;l++){
			Record r=dr.getRecord(l);
			
			int itag=dd.getXNum(r.getXPos());
			int jtag=dd.getYNum(r.getYPos());
			int ltag=ptrs[jtag][itag];
			
			rs[jtag][itag][ltag]=r;
			
			ptrs[jtag][itag]++;
		}
		
		return rs;
	}
	
	private static long[] cTimes(MDate mdstr,String incre,MDate mdend){
		MDate mdtmp=mdstr.add(incre);
		
		int dt=mdstr.getDT(mdtmp);
		
		int len=mdstr.getDT(mdend)/dt+1;
		
		long[] ltims=new long[len];
		
		ltims[0]=mdstr.getLongTime();
		
		for(int l=1;l<len;l++){
			ltims[l]=mdtmp.getLongTime();
			mdtmp=mdtmp.addSeconds(dt);
		}
		
		return ltims;
	}
	
	private static void processInvalidDrifter(List<GDPDrifter> all){
		for(GDPDrifter drftr:all){
			if(drftr.getID().equals("11582530")){
				System.out.print("found drifter 11582530...");
				boolean corrected=false;
				
				for(int l=0,L=drftr.getTCount();l<L;l++){
					long time=drftr.getTime(l);
					
					if(time<20130401000000L){
						corrected=true;
						Record r=drftr.getRecord(l);
						
						r.setData(0,Record.undef);
						r.setData(1,Record.undef);
					}
				}
				
				if(corrected) System.out.println("  corrected!");
				else System.out.println("  not corrected.");
			}
			
			if(drftr.getID().equals("70849")){
				System.out.print("found drifter 70849...");
				boolean corrected=false;
				
				for(int l=0,L=drftr.getTCount();l<L;l++){
					long time=drftr.getTime(l);
					
					if(time>20090621000000L&&time<20090629000000L){
						corrected=true;
						Record r=drftr.getRecord(l);
						
						r.setData(0,Record.undef);
						r.setData(1,Record.undef);
					}
				}
				
				if(corrected) System.out.println("  corrected!");
				else System.out.println("  not corrected.");
			}
			
			if(drftr.getID().equals("36165")){
				System.out.print("found drifter 36165...");
				boolean corrected=false;
				
				for(int l=0,L=drftr.getTCount();l<L;l++){
					long time=drftr.getTime(l);
					
					if(time>20080630000000L&&time<20090109000000L){
						corrected=true;
						Record r=drftr.getRecord(l);
						
						r.setData(0,Record.undef);
						r.setData(1,Record.undef);
					}
				}
				
				if(corrected) System.out.println("  corrected!");
				else System.out.println("  not corrected.");
			}
			
			if(drftr.getID().equals("53936")){
				System.out.print("found drifter 53936...");
				boolean corrected=false;
				
				for(int l=0,L=drftr.getTCount();l<L;l++){
					long time=drftr.getTime(l);
					
					if(time>20120101000000L){
						corrected=true;
						Record r=drftr.getRecord(l);
						
						r.setData(0,Record.undef);
						r.setData(1,Record.undef);
					}
				}
				
				if(corrected) System.out.println("  corrected!");
				else System.out.println("  not corrected.");
			}
			
			if(drftr.getID().equals("15705")){
				System.out.print("found drifter 15705...");
				boolean corrected=false;
				
				for(int l=0,L=drftr.getTCount();l<L;l++){
					long time=drftr.getTime(l);
					
					if(time>20001120000000L){
						corrected=true;
						Record r=drftr.getRecord(l);
						
						r.setData(0,Record.undef);
						r.setData(1,Record.undef);
					}
				}
				
				if(corrected) System.out.println("  corrected!");
				else System.out.println("  not corrected.");
			}
			
			if(drftr.getID().equals("9705880")){
				System.out.print("found drifter 9705880...");
				boolean corrected=false;
				
				for(int l=0,L=drftr.getTCount();l<L;l++){
					long time=drftr.getTime(l);
					
					if(time>19980117000000L){
						corrected=true;
						Record r=drftr.getRecord(l);
						
						r.setData(0,Record.undef);
						r.setData(1,Record.undef);
					}
				}
				
				if(corrected) System.out.println("  corrected!");
				else System.out.println("  not corrected.");
			}
			
			if(drftr.getID().equals("89770")){
				System.out.print("found drifter 89770...");
				boolean corrected=false;
				
				for(int l=0,L=drftr.getTCount();l<L;l++){
					long time=drftr.getTime(l);
					
					if(time>20120322000000L){
						corrected=true;
						Record r=drftr.getRecord(l);
						
						r.setData(0,Record.undef);
						r.setData(1,Record.undef);
					}
				}
				
				if(corrected) System.out.println("  corrected!");
				else System.out.println("  not corrected.");
			}
			
			if(drftr.getID().equals("63817")){
				System.out.print("found drifter 63817...");
				boolean corrected=false;
				
				for(int l=0,L=drftr.getTCount();l<L;l++){
					long time=drftr.getTime(l);
					
					if(time>20080914000000L){
						corrected=true;
						Record r=drftr.getRecord(l);
						
						r.setData(0,Record.undef);
						r.setData(1,Record.undef);
					}
				}
				
				if(corrected) System.out.println("  corrected!");
				else System.out.println("  not corrected.");
			}
			
			if(drftr.getID().equals("81824")){
				System.out.print("found drifter 81824...");
				boolean corrected=false;
				
				for(int l=0,L=drftr.getTCount();l<L;l++){
					long time=drftr.getTime(l);
					
					if(time>20120709000000L){
						corrected=true;
						Record r=drftr.getRecord(l);
						
						r.setData(0,Record.undef);
						r.setData(1,Record.undef);
					}
				}
				
				if(corrected) System.out.println("  corrected!");
				else System.out.println("  not corrected.");
			}
			
			if(drftr.getID().equals("81824")){
				System.out.print("found drifter 81824...");
				boolean corrected=false;
				
				for(int l=0,L=drftr.getTCount();l<L;l++){
					long time=drftr.getTime(l);
					
					if(time>20120709000000L){
						corrected=true;
						Record r=drftr.getRecord(l);
						
						r.setData(0,Record.undef);
						r.setData(1,Record.undef);
					}
				}
				
				if(corrected) System.out.println("  corrected!");
				else System.out.println("  not corrected.");
			}
			
			if(drftr.getID().equals("40275")){
				System.out.print("found drifter 40275...");
				boolean corrected=false;
				
				for(int l=0,L=drftr.getTCount();l<L;l++){
					long time=drftr.getTime(l);
					
					if(time>20080214000000L){
						corrected=true;
						Record r=drftr.getRecord(l);
						
						r.setData(0,Record.undef);
						r.setData(1,Record.undef);
					}
				}
				
				if(corrected) System.out.println("  corrected!");
				else System.out.println("  not corrected.");
			}
			
			if(drftr.getID().equals("40273")){
				System.out.print("found drifter 40273...");
				boolean corrected=false;
				
				for(int l=0,L=drftr.getTCount();l<L;l++){
					long time=drftr.getTime(l);
					
					if(time>20080805000000L){
						corrected=true;
						Record r=drftr.getRecord(l);
						
						r.setData(0,Record.undef);
						r.setData(1,Record.undef);
					}
				}
				
				if(corrected) System.out.println("  corrected!");
				else System.out.println("  not corrected.");
			}
		}
	}
	
	
	/** test
	public static void main(String[] args){
		
	}*/
}
