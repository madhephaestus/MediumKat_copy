import java.util.ArrayList;

import com.neuronrobotics.sdk.addons.kinematics.DHChain;
import com.neuronrobotics.sdk.addons.kinematics.DHLink;
import com.neuronrobotics.sdk.addons.kinematics.DhInverseSolver;
import com.neuronrobotics.sdk.addons.kinematics.math.TransformNR;
import com.neuronrobotics.sdk.common.Log;					
import Jama.Matrix;

return new DhInverseSolver() {
	
	@Override
	public double[] inverseKinematics(TransformNR target,
			double[] jointSpaceVector,DHChain chain ) {
		ArrayList<DHLink> links = chain.getLinks();
		if(links.size()<3){
			return jointSpaceVector
		}
		// THis is the jacobian for the given configuration
		//Matrix jacobian =  chain.getJacobian(jointSpaceVector);
		Matrix taskSpacMatrix = target.getMatrixTransform();
		
		int linkNum = jointSpaceVector.length;
		double [] inv = new double[linkNum];
		if(linkNum<3){
			for(int i=i;i<linkNum;i++){
				inv[i]=jointSpaceVector[i]+0.0001
			}
		}
		// this is an ad-hock kinematic model for d-h parameters and only works for specific configurations
		
		double d = links.get(1).getD()- links.get(2).getD();
		double r = links.get(0).getR();

		double lengthXYPlaneVect = Math.sqrt(Math.pow(target.getX(),2)+Math.pow(target.getY(),2));
		double angleXYPlaneVect = Math.asin(target.getY()/lengthXYPlaneVect);
		
		double angleRectangleAdjustedXY =Math.asin(d/lengthXYPlaneVect);
		
		double lengthRectangleAdjustedXY = lengthXYPlaneVect* Math.cos(angleRectangleAdjustedXY)-r;
		
		
		double orentation = angleXYPlaneVect-angleRectangleAdjustedXY;
		if(Math.abs(Math.toDegrees(orentation))<0.01){
			orentation=0;
		}
		double ySet = lengthRectangleAdjustedXY*Math.sin(orentation);
		double xSet = lengthRectangleAdjustedXY*Math.cos(orentation);
	
		
		double zSet = target.getZ() - links.get(0).getD();
		if(links.size()==5){
			double tipAngulationSum =Math.toDegrees( links.get(1).getTheta()+
								links.get(2).getTheta()+
								links.get(4).getTheta())
			//println "Tip angulation orentation = "+tipAngulationSum
			if(tipAngulationSum==90)
				zSet+=links.get(4).getD();
			else{
				double tipySet = links.get(4).getR()*Math.sin(orentation);
				double tipxSet = links.get(4).getR()*Math.cos(orentation);
				//println "5 links back Setting tip x="+tipxSet+" y="+tipySet
				xSet-=tipxSet
				ySet-=tipySet
			}
		}
		if(links.size()==4){
			double tipAngulationSum =Math.toDegrees( links.get(1).getTheta()+
								links.get(2).getTheta()+
								links.get(3).getTheta())
			//println "Tip angulation orentation = "+tipAngulationSum
			if(tipAngulationSum==90)
				zSet+=links.get(3).getR();
			else{
				double tipySet = links.get(3).getR()*Math.sin(orentation);
				double tipxSet = links.get(3).getR()*Math.cos(orentation);
				//println "4 links back Setting tip x="+tipxSet+" y="+tipySet
				xSet-=tipxSet
				ySet-=tipySet
			}
		}
		// Actual target for anylitical solution is above the target minus the z offset
		TransformNR overGripper = new TransformNR(
				xSet,
				ySet,
				zSet,
				target.getRotation());


		double l1 = links.get(1).getR();// First link length
		double l2 = links.get(2).getR();

		double vect = Math.sqrt(xSet*xSet+ySet*ySet+zSet*zSet);
		/*
		println ( "TO: "+target);
		println ( "Trangular TO: "+overGripper);
		println ( "lengthXYPlaneVect: "+lengthXYPlaneVect);
		println( "angleXYPlaneVect: "+Math.toDegrees(angleXYPlaneVect));
		println( "angleRectangleAdjustedXY: "+Math.toDegrees(angleRectangleAdjustedXY));
		println( "lengthRectangleAdjustedXY: "+lengthRectangleAdjustedXY);
		println( "r: "+r);
		println( "d: "+d);
		
		println( "x Correction: "+xSet);
		println( "y Correction: "+ySet);
		
		println( "Orentation: "+Math.toDegrees(orentation));
		println( "z: "+zSet);
	*/
		

		if (vect > l1+l2 ||  vect<0 ||lengthRectangleAdjustedXY<0 ) {
			throw new RuntimeException("Hypotenus too long: "+vect+" longer then "+l1+l2);
		}
		//from https://www.mathsisfun.com/algebra/trig-solving-sss-triangles.html
		double a=l2;
		double b=l1;
		double c=vect;
		double A =Math.acos((Math.pow(b,2)+ Math.pow(c,2) - Math.pow(a,2)) / (2.0*b*c));
		double B =Math.acos((Math.pow(c,2)+ Math.pow(a,2) - Math.pow(b,2)) / (2.0*a*c));
		double C =Math.PI-A-B;//Rule of triangles
		double elevation = Math.asin(zSet/vect);

/*
		println( "vect: "+vect);
		println( "A: "+Math.toDegrees(A));
		println( "elevation: "+Math.toDegrees(elevation));
		println( "l1 from x/y plane: "+Math.toDegrees(A+elevation));
		println( "l2 from l1: "+Math.toDegrees(C));
		*/
		inv[0] = Math.toDegrees(orentation);
		inv[1] = -Math.toDegrees((A+elevation+links.get(1).getTheta()));
		if((int)links.get(1).getAlpha() ==180){
			inv[2] = (Math.toDegrees(C))-180-//interior angle of the triangle, map to external angle
					Math.toDegrees(links.get(2).getTheta());// offset for kinematics
		}
		if((int)links.get(1).getAlpha() ==0){
			inv[2] = -(Math.toDegrees(C))+Math.toDegrees(links.get(2).getTheta());// offset for kinematics
		}
		if(links.size()>3)
			inv[3] =-(inv[1] + inv[2]);// keep it parallell
			// We know the wrist twist will always be 0 for this model
		if(links.size()>4)
			inv[4] = inv[0];//keep the tool orentation paralell from the base
		
		for(int i=0;i<inv.length;i++){
			if(Math.abs(inv[i]) < 0.01){
				inv[i]=0;
			}
//			println( "Link#"+i+" is set to "+inv[i]);
		}
		int i=3;
		if(links.size()>3)
			i=5;
		//copy over remaining links so they do not move
		for(;i<inv.length && i<jointSpaceVector.length ;i++){
			inv[i]=jointSpaceVector[i];
		}
		return inv;
	}
};
