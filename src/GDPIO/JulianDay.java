package GDPIO;

import miniufo.diagnosis.MDate;


public class JulianDay{
	//
	public static void main(String[] args){
		MDate ref=new MDate(1979,1,1);
		
		System.out.println("id==11582530, "+ref.addDays(12510-1));
		System.out.println("id==70849, "+ref.addDays(11130-1)+"\t"+ref.addDays(11138-1));
		System.out.println("id==36165, "+ref.addDays(10774-1)+"\t"+ref.addDays(10967-1));
		//System.out.println("id==36165, "+ref.addDays(10822-1)+"\t"+ref.addDays(10898-1));
		//System.out.println("id==36165, "+ref.addDays(10921-1)+"\t"+ref.addDays(10953-1));
		System.out.println("id==53936, "+ref.addDays(12054-1));
		System.out.println("id==15705, "+ref.addDays(7995-1));
		System.out.println("id==9705880, "+ref.addDays(6957-1));
		System.out.println("id==89770, "+ref.addDays(12135-1));
		System.out.println("id==63817, "+ref.addDays(10850-1));
		System.out.println("id==81824, "+ref.addDays(12244-1));
		System.out.println("id==40275, "+ref.addDays(10637-1));
		System.out.println("id==40273, "+ref.addDays(10810-1));
	}
}
