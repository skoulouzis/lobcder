package com.bradmcevoy.http;

public class Range {

	private final long start;
	private final long finish;

	public static Range parse(String s) {
		String[] arr = s.split("-");
		if (arr.length != 2) {
			throw new RuntimeException("Invalid range. Use format start-finish, eg 1000-1500. Range:" + s);
		}
		try {
			Integer start = Integer.parseInt(arr[0]);
			Integer finish = Integer.parseInt(arr[1]);
			return new Range(start, finish);
		} catch (Throwable e) {
			throw new RuntimeException("Invalid range. Use format start-finish, eg 1000-1500. Range:" + s);
		}
	}

	public Range(long start, long finish) {
		this.start = start;
		this.finish = finish;
	}

	public long getStart() {
		return start;
	}

	public long getFinish() {
		return finish;
	}

	@Override
	public String toString() {
		return "bytes " + start + "-" + finish;
	}

	/**
	 * Returns range in String format ("start-end"), ready to be put into
	 * HTTP range request
	 * @return Range of data in stream
	 */
	public String getRange() {
		return start + "-" + finish;
	}
}
