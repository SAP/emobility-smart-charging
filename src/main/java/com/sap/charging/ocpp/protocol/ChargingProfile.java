package com.sap.charging.ocpp.protocol;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ChargingProfile {

	/**
	 * Required. Unique identifier for this profile.
	 */
	public final int chargingProfileId;
	
	/**
	 * Optional. Only valid if ChargingProfilePurpose is set to TxProfile,
		the transactionId MAY be used to match the profile to a specific
		transaction.
	 */
	//private final int transactionId;
	
	/**
	 * Required. Value determining level in hierarchy stack of profiles.
		Higher values have precedence over lower values. Lowest level is
		0.
		
		Here: Always 0
	 */
	public final int stackLevel = 0;
	
	
	/**
	 * Required. Defines the purpose of the schedule transferred by this
		message.
		
		Here: Always ChargePointMaxProfile
	 */
	public final ChargingProfilePurposeType chargingProfilePurpose = ChargingProfilePurposeType.ChargePointMaxProfile;
	
	/**
	 * Required. Indicates the kind of schedule (absolute, recurring, relative).
	 * 
	 * Here: Always Absolute
	 */
	public final ChargingProfileKindType chargingProfileKind = ChargingProfileKindType.Absolute;
	
	/**
	 * Optional. Indicates the start point of a recurrence.
	 */
	// private final RecurrencyKindType recurrendyKind;
	
	
	// private final LocalDateTime validFrom
	// private final LocalDateTime validTo
	
	public final ChargingSchedule chargingSchedule;
	
	
	@JsonCreator
	public ChargingProfile(
			@JsonProperty("chargingProfileId") int chargingProfileId,
			@JsonProperty("chargingSchedule") ChargingSchedule chargingSchedule
			) {
		this.chargingProfileId = chargingProfileId;
		this.chargingSchedule = chargingSchedule;
	}
	
	public ChargingProfile(ChargingSchedule chargingSchedule) {
		this(getRandomChargingProfileId(), chargingSchedule);
	}
	
	
	public static int getRandomChargingProfileId() {
		return (int)(Math.random() * Integer.MAX_VALUE);
	}
	
	
	
	
	
	
	
}
