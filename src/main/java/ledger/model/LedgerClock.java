package ledger.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

import static ledger.util.DateTimeUtil.DB_SAFE_LOCAL_DATETIME_MIN;


@Getter
@AllArgsConstructor
@NoArgsConstructor
public class LedgerClock {
    /*
     * When applying ledger activities, we need to keep a track of the current time.
     * The real clock cannot be used because we often do back-dated entries, and occasionally we need to recreate a
     * ledger
     * by replaying the past events.
     */
    private LocalDateTime now = DB_SAFE_LOCAL_DATETIME_MIN;
    // We keep a list of applied activities to avoid reapplying the same activity

    public void advanceTime(LocalDateTime time) {
        if (time == null) {
            throw new IllegalArgumentException("now cannot be null");
        }

        if (time.isAfter(this.now)) {
            this.now = time;
        } // Else, we do not want to go back in time
    }
}