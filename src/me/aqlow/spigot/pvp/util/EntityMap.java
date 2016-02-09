package me.aqlow.spigot.pvp.util;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;

import org.bukkit.entity.Entity;

public class EntityMap<K extends Entity, V> {
	private HashMap<WeakReference<K>, V> map = new HashMap<WeakReference<K>, V>();
	
	public void put(K key, V value) {
		this.remove(key);
		WeakReference<K> ref = new WeakReference<K>(key);
		map.put(ref, value);
	}
	
	public V get(K key) {
		for(WeakReference<K> ref : map.keySet()) {
			K k = ref.get();
			if(k == null) {
				continue;
			}
			if(k.getUniqueId().equals(key.getUniqueId())) {
				return map.get(ref);
			}
		}
		
		return null;
	}
	
	public ArrayList<K> reverseGet(V value) {
		ArrayList<K> list = new ArrayList<K>();
		
		for(WeakReference<K> ref : map.keySet()) {
			K k = ref.get();
			if(k == null) {
				continue;
			}
			if(map.get(ref).equals(value)) {
				list.add(k);
			}
		}
		
		return list;
	}
	
	public void remove(K key) {
		ArrayList<WeakReference<K>> toRemove = new ArrayList<WeakReference<K>>();
		for(WeakReference<K> ref : map.keySet()) {
			K k = ref.get();
			if(k == null || k.isDead()) {
				toRemove.add(ref);
				continue;
			}
			if(k.getUniqueId().equals(key.getUniqueId())) {
				toRemove.add(ref);
				continue;
			}
		}
		
		for(WeakReference<K> ref : toRemove) {
			map.remove(ref);
		}
	}
	
	public boolean containsKey(K key) {
		for(WeakReference<K> ref : map.keySet()) {
			K k = ref.get();
			if(k == null) {
				continue;
			}
			if(k.getUniqueId().equals(key.getUniqueId())) {
				return true;
			}
		}
		
		return false;
	}
	
	public ArrayList<K> getAll() {
		ArrayList<K> all = new ArrayList<K>();
		
		for(WeakReference<K> ref : map.keySet()) {
			K key = ref.get();
			
			if(key != null) {
				all.add(key);
			}
		}
		
		return all;
	}
	
	public void clear() {
		map.clear();
	}
}
