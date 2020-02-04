package com.sap.charging.ocpp.protocol;

public enum ChargingProfilePurposeType {
	
	/**
	 * Configuration for the maximum power or current available for an entire Charge Point.
	 * 
	 * Here: Always used
	 */
	ChargePointMaxProfile,
	
	/**
	 * Default profile *that can be configured in the Charge Point. When a new transaction is started, this profile
		SHALL be used, unless it was a transaction that was started by a RemoteStartTransaction.req with a
		ChargeProfile that is accepted by the Charge Point.
	 */
	TxDefaultProfile,
	
	/**
	 * Profile with constraints to be imposed by the Charge Point on the current transaction, or on a new transaction
		when this is started via a RemoteStartTransaction.req with a ChargeProfile. A profile with this purpose SHALL
		cease to be valid when the transaction terminates.
	 */
	TxProfile
	
	
	
}
