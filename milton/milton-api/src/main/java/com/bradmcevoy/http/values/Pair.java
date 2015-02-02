package com.bradmcevoy.http.values;

/**
 * Represents an object that constitutes with two related objects. 
 * 
 * Example: 
 * Xml Attribtes -> name,value
 * 
 * @author nabil.shams
 */
public class Pair<T,U> {
	private T object1;
	private U object2;
	public Pair(T t, U u){
		object1 = t;
		object2 = u;
	}

	/**
	 * @return the object1
	 */
	public T getObject1() {
		return object1;
	}

	/**
	 * @return the object2
	 */
	public U getObject2() {
		return object2;
	}
}
