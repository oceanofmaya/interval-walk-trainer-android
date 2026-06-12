# Privacy Policy

**Last Updated:** June 2026

## Introduction

Interval Walk Trainer ("we," "our," or "the app") is committed to protecting your privacy. This Privacy Policy explains how we handle information when you use our Android application.

## Information We Collect

### No Personal Data Collection

Interval Walk Trainer does **not** collect, transmit, or share any personal information. The app operates entirely offline and does not connect to any external servers or services.

### Local Data Storage

The app stores the following preferences locally on your device using Android's SharedPreferences:

- **Theme Preference**: Your choice of dark or light theme
- **Vibration Setting**: Whether vibration notifications are enabled
- **Voice Setting**: Whether voice notifications are enabled
- **Use Health Connect Setting**: Whether optional Health Connect metrics are enabled
- **Weekly Goal Settings**: Optional weekly targets and reminder preferences, including selected reminder days and time

Additionally, the app stores workout history and optional saved workout templates locally on your device using a local database:

- **Workout History**: Dates on which you completed workouts, the number of workouts completed per day, total minutes walked per day, and optional per-session Health Connect metrics when enabled and available. This includes statistics such as workout streaks, total workouts, total minutes walked, step counts, and heart-rate summaries.
- **Saved Presets**: Names and interval/circuit settings for up to 30 custom presets you choose to keep in the formula picker. They are not uploaded or synced.

This data is stored only on your device and is never transmitted or shared with us or any third parties. All workout history data remains completely private and local to your device.

### Runtime Data

During app usage, the app temporarily stores timer state (current interval, time remaining, phase) in memory. This data is not persisted and is cleared when you close the app or reset the timer.

### Health and Fitness Data

The app stores workout completion data locally on your device, including:

- Dates when workouts were completed
- Number of workouts completed per day
- Total minutes walked per day
- Optional step counts and heart-rate summaries for completed workouts when Use Health Connect is enabled and Health Connect data is available

This health and fitness data is stored locally using Android's Room database and is never transmitted, synced to the cloud, or shared with any third parties. You have full control over this data, and it can be completely removed by uninstalling the app.

When you complete a workout, the app records the completion date, number of workouts for that day, total minutes walked, and optional metrics if enabled and available. This data is stored locally in a database on your device and is used only to display your workout history and statistics within the app.

## Permissions

The app requests the following permissions:

- **VIBRATE**: Used to provide vibration notifications during interval phase changes. This permission does not collect or transmit any data.
- **POST_NOTIFICATIONS**: Required on Android 13+ to display system notifications. The app only shows timer-related notifications and does not collect data through notifications.
- **SCHEDULE_EXACT_ALARM**: Used for optional weekly goal reminders so Android can show them at the exact time you choose. This permission does not collect, store, transmit, or share any data.
- **WAKE_LOCK**: Used to keep the timer running accurately when your device screen is locked. This ensures the timer continues functioning in the background without collecting any data.
- **FOREGROUND_SERVICE**: Required by Android to run an active workout as a foreground service so interval timing can continue while the app is in the background.
- **FOREGROUND_SERVICE_HEALTH**: Required by Android for health/fitness foreground-service use cases on newer Android versions.
- **ACTIVITY_RECOGNITION**: Required by Android when starting a health foreground service on Android 14+.
  - This permission is used only to satisfy Android platform requirements for background workout execution.
- **Health Connect step and heart-rate read access**: Used only when Use Health Connect is enabled and permission is granted, so the app can read step counts and heart-rate samples for the completed workout window and save summaries locally.
  - The app reads Health Connect data only; it does not write workouts or metrics back to Health Connect.
  - If Health Connect is unavailable, permission is denied, or no samples exist for the workout window, Health Connect summaries are omitted.

## Third-Party Services

Interval Walk Trainer does not use any third-party analytics, advertising, or tracking services. The app does not integrate with social media platforms or other external services.

### System Services

The app uses the following Android system services, which are provided by your device's operating system:

- **Text-to-Speech (TTS)**: Used for optional voice notifications. TTS is a system service that runs locally on your device. We do not have access to any data processed by TTS.
- **Vibration Service**: Used for vibration notifications. This service operates locally on your device.
- **Alarm Manager**: Used for optional weekly goal reminders. Reminder settings remain local on your device.

## Data Sharing

We do not share, sell, or transmit any data to third parties because we do not collect any data.

## Data Security

Since the app does not collect or transmit personal data, there is no risk of data breaches or unauthorized access to personal information. All local preferences are stored securely on your device using Android's standard secure storage mechanisms.

## Children's Privacy

Interval Walk Trainer is intended for adults and is listed on Google Play for users age 18 and older. The app is not directed to children or minors, and we do not knowingly collect personal information from children. Because the app does not collect or transmit personal data, there is no child personal information for us to receive, store on our servers, sell, or share.

## Your Rights

Since we do not collect personal data, there is no data to access, modify, or delete from our servers. All app preferences and workout history stored locally on your device can be cleared by uninstalling the app. The app provides functionality to view your workout history and statistics, but this data never leaves your device.

### Deleting Your Workout Data

You can delete all your workout history and statistics at any time directly from within the app. The app includes a "Clear All" feature in the Stats screen that allows you to permanently delete all workout records, including:

- All workout completion dates
- All workout counts and minutes tracked
- All statistics (streaks, totals, averages)

This action is permanent and cannot be undone. After clearing your data, you can continue using the app and new workouts will be tracked from that point forward.

## Changes to This Privacy Policy

We may update this Privacy Policy from time to time. Any changes will be posted on this page with an updated "Last Updated" date. We encourage you to review this Privacy Policy periodically.

## Contact Us

If you have any questions about this Privacy Policy, please contact us through the app's support channels or GitHub repository.

For support or privacy-related inquiries, please open an issue on the app's GitHub repository or contact the developer through the contact information provided in the Google Play Store listing.

## Compliance

This Privacy Policy complies with:

- Google Play Store requirements
- General Data Protection Regulation (GDPR)
- California Consumer Privacy Act (CCPA)
- Other applicable privacy laws

---

**Note**: This privacy policy reflects the current functionality of Interval Walk Trainer. Since the app does not collect, transmit, or share any data, this policy is straightforward and transparent about our privacy practices.