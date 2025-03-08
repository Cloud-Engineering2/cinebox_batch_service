package cinebox_batch_service.enums;

import lombok.Getter;

@Getter
public enum RatingGrade {
	ALL("전체관람가", 0),
	AGE_12("12세이상관람가", 12),
	AGE_15("15세이상관람가", 15),
	AGE_18("청소년관람불가", 18),
	NOT_SET("등급미설정", 0);

	private final String label;
	private final int minAge;

	RatingGrade(String label, int minAge) {
		this.label = label;
		this.minAge = minAge;
	}

	public static RatingGrade fromLabel(String label) {
		if (label == null) {
			return NOT_SET;
		}
		for (RatingGrade rating : values()) {
			if(rating.getLabel().equals(label)) {
				return rating;
			}
		}
		
		return NOT_SET;
	}
}
