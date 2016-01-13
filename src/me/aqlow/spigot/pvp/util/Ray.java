package me.aqlow.spigot.pvp.util;

import org.bukkit.util.Vector;

public final class Ray {
	private final double inverseDirX;
	private final double inverseDirY;
	private final double inverseDirZ;
	private final Vector pos;
	private final Vector direction;
	
	public Ray(Vector pos, Vector dir) {
		this.direction = dir.clone();
		dir = dir.clone().normalize();
		inverseDirX = safeDivide(1, dir.getX());
		inverseDirY = safeDivide(1, dir.getY());
		inverseDirZ = safeDivide(1, dir.getZ());
		this.pos = pos;
	}
	
	// source: http://gamedev.stackexchange.com/a/18459
	public double collide(AABB aabb) {
		double dxMin = (aabb.getMinimum().getX() - pos.getX()) * inverseDirX;
		double dxMax = (aabb.getMaximum().getX() - pos.getX()) * inverseDirX;
		double dyMin = (aabb.getMinimum().getY() - pos.getY()) * inverseDirY;
		double dyMax = (aabb.getMaximum().getY() - pos.getY()) * inverseDirY;
		double dzMin = (aabb.getMinimum().getZ() - pos.getZ()) * inverseDirZ;
		double dzMax = (aabb.getMaximum().getZ() - pos.getZ()) * inverseDirZ;
		
		double dmin = Math.max(Math.max(Math.min(dxMin, dxMax), Math.min(dyMin, dyMax)), Math.min(dzMin, dzMax));
		double dmax = Math.min(Math.min(Math.max(dxMin, dxMax), Math.max(dyMin, dyMax)), Math.max(dzMin, dzMax));
		
		if(dmax < 0) {
			return -1;
		}
		else if(dmin > dmax) {
			return -1;
		}
		else {
			return dmin;
		}
	}
	
	private double safeDivide(double num, double div) {
		if(div == 0) return Double.POSITIVE_INFINITY;
		
		return num / div;
	}
	
	public Vector getPosition() {
		return pos;
	}
	
	public Vector getDirection() {
		return direction;
	}
	
	public double getLength() {
		return direction.length();
	}
}
