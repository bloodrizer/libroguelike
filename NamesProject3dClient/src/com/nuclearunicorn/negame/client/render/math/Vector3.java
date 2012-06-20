/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nuclearunicorn.negame.client.render.math;
import org.lwjgl.util.vector.Vector3f;

public final class Vector3 {

        public static Vector3 util_vec3 = new Vector3(0,0,0);

	/**
	 * Returns a vector with the given values.
	 *
	 * @param x
	 *            The x component of the vector.
	 * @param y
	 *            The y component of the vector.
	 * @param z
	 *            The z component of the vector.
	 */
	public static Vector3 valueOf(int x, int y, int z) {
		return new Vector3(x, y, z);
	}

        public Vector3 set(int x, int y, int z) {
            this.x = x;
            this.y = y;
            this.z = z;
            
            return this;
        }

        public Vector3 set(Vector3f vec){
            this.x = (int)vec.x;
            this.y = (int)vec.y;
            this.z = (int)vec.z;
            return this;
        }

        public Vector3 set(Vector3 vec){
            this.x = vec.x;
            this.y = vec.y;
            this.z = vec.z;

            return this;
        }

	/** Lazily initialized, cached hash code. */
	private volatile int hashCode;

	/** The x component of this vector. */
	public int x;

	/** The y component of this vector. */
	public int y;

	/** The z component of this vector. */
	public int z;

	/**
	 * @param x
	 *            The x component of the vector.
	 * @param y
	 *            The y component of the vector.
	 * @param z
	 *            The z component of the vector.
	 */
	public Vector3(int x, int y, int z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public int x() {
		return x;
	}

	/**
	 * Returns the y component of this vector.
	 */
	public int y() {
		return y;
	}

	/**
	 * Returns the z component of this vector.
	 */
	public int z() {
		return z;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof Vector3)) {
			return false;
		}
		Vector3 otherVector = (Vector3) obj;
		return x == otherVector.x && y == otherVector.y && z == otherVector.z;
	}

	@Override
	public int hashCode() {
		int result = hashCode;
		if (result == 0) {
			result = (x%100)*10000 + (y%100)*100 + (z%100);
			//result = (x)*10000 + (y)*100 + (z);
		}
		return result;
	}

	@Override
	public String toString() {
		return "Vector3 [" + x + ", " + y + ", " + z + "]";
	}
}
