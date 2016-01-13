package me.aqlow.spigot.pvp.util;

import org.bukkit.util.Vector;

import net.minecraft.server.v1_8_R3.AxisAlignedBB;

public final class AABB {
	private final Vector minimum;
	private final Vector maximum;
	
	public AABB(Vector minimum, Vector maximum) {
		this.minimum = minimum;
		this.maximum = maximum;
	}
	
	public AABB(AxisAlignedBB nms) {
		minimum = new Vector(nms.a, nms.b, nms.c);
		maximum = new Vector(nms.d, nms.e, nms.f);
	}
	
	public Vector getMinimum() {
		return minimum;
	}
	
	public Vector getMaximum() {
		return maximum;
	}
	
	public double distanceToPoint(Vector point) {
		double xDist = distanceToPoint1D(point.getX(), minimum.getX(), maximum.getX());
		double yDist = distanceToPoint1D(point.getY(), minimum.getY(), maximum.getY());
		double zDist = distanceToPoint1D(point.getZ(), minimum.getZ(), maximum.getZ());
		return Math.sqrt(xDist * xDist + yDist * yDist + zDist * zDist);
	}
	
	private double distanceToPoint1D(double point, double min, double max) {
		if(point < min) {
			return min - point;
		}
		else if(point > max) {
			return point - max;
		}
		else {
			return 0;
		}
	}
	
	public AABB expand(double radius) {
		Vector diff = new Vector(radius, radius, radius);
		return new AABB(minimum.subtract(diff), maximum.add(diff));
	}
}
