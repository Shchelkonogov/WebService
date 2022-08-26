package ru.tn.server.model;

import java.time.LocalDateTime;
import java.util.StringJoiner;

/**
 * @author Maksim Shchelkonogov
 */
public class LoadDataModel {

    private long muid;
    private LocalDateTime startDate;

    public LoadDataModel(long muid, LocalDateTime startDate) {
        this.muid = muid;
        this.startDate = startDate;
    }

    public long getMuid() {
        return muid;
    }

    public LocalDateTime getStartDate() {
        return startDate;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", LoadDataModel.class.getSimpleName() + "[", "]")
                .add("muid=" + muid)
                .add("startDate=" + startDate)
                .toString();
    }
}
