package com.plummy.vio.core.section;

public class ActivityDescriptionSection implements Section {

    private final String activityDescription;

    public ActivityDescriptionSection(String activityDescription) {
        this.activityDescription = activityDescription;
    }

    @Override
    public SectionType getType() {
        return SectionType.ACTIVITY_DESCRIPTION;
    }

    @Override
    public <T> T accept(SectionVisitor<T> visitor) {
        return visitor.visitActivityDescriptionSection(this);
    }

    public String getActivityDescription() {
        return activityDescription;
    }
}
