package org.wheatgenetics.onekk;

public class InventoryRecord {

	private int id;
	private String mBoxID;
	private String mEnvID;
	private String mPersonID;
	private String mDate;
	private int mPosition;
	private String mWt;

	public InventoryRecord() {
	}

	public InventoryRecord(String mBoxID, String mEnvID, String mPersonID, String mDate,
			int mPosition, String mWt) {
		super();
		this.mBoxID = mBoxID;
		this.mEnvID = mEnvID;
		this.mPersonID = mPersonID;
		this.mDate = mDate;
		this.mPosition = mPosition;
		this.mWt = mWt;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getBox() {
		return mBoxID;
	}

	public void setBox(String title) {
		this.mBoxID = title;
	}

	public String getEnvID() {
		return mEnvID;
	}

	public void setEnvID(String author) {
		this.mEnvID = author;
	}

	public String getPersonID() {
		return mPersonID;
	}

	public void setPersonID(String author) {
		this.mPersonID = author;
	}

	
	public void setDate(String author) {
		this.mDate = author;
	}

	public String getDate() {
		return mDate;
	}

	public int getPosition() {
		return mPosition;
	}

	public void setPosition(int author) {
		this.mPosition = author;
	}

	public String getWt() {
		return mWt;
	}

	public void setWt(String author) {
		this.mWt = author;
	}

	@Override
	public String toString() {
		return mBoxID + "," + mEnvID + "," + mPersonID + "," + mDate + "," + mPosition + "," + mWt;
	}
}
