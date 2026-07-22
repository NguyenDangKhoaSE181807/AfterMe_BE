# Passive Activity Check-in Flow

## Business rule

Passive Activity Assist dung tin hieu hoat dong gan day tren ung dung de giam transition check-in va giam false alarm, nhung khong bao gio tu dong xem nguoi dung la an toan.

Tin hieu phase 1:

- `APP_FOREGROUND`: nguoi dung vua mo AfterMe.
- `APP_INTERACTION`: nguoi dung vua thao tac trong AfterMe.
- `PUSH_TAPPED`: nguoi dung vua bam thong bao AfterMe.

Nhung tin hieu nhu `DEVICE_UNLOCKED`, `DEVICE_INTERACTIVE`, `MOTION_DETECTED` chi nen dung o phase sau vi do tin cay thap hon va co van de privacy/battery.

### Smart check-in window theo activity

Lich check-in co dinh van la source of truth, nhung app co the hien check-in linh hoat hon neu thay user dang hoat dong gan thoi diem check-in.

Rule phase 1:

- `scheduledTime` van la gio chinh thuc cua daily check-in.
- Backend cho check-in som trong 30 phut truoc `scheduledTime` doi voi system daily check-in.
- FE nen hien prompt manh trong khoang `scheduledTime - 30 minutes` den `scheduledTime + 60 minutes` neu co tin hieu hoat dong.
- User van co the check-in cho den `responseDeadline` nhu flow cu.
- Truoc `scheduledTime - 30 minutes`, backend van tu choi check-in de tranh viec check-in qua som.

Vi du:

- User dat daily check-in luc 08:00.
- Luc 07:35 user mo app hoac bam push.
- FE thay `canCheckInNow = true` va `checkInPromptReason = EARLY_ACTIVITY_WINDOW`.
- FE hien nut check-in ngay trong app.
- Neu user xac nhan, instance 08:00 duoc xem la da check-in an toan.

Y nghia:

- Khong can tao them transition chi vi user dang dung may gan gio check-in.
- Khong tu dong danh dau an toan bang sensor.
- Sensor/activity chi giup hien prompt dung luc hon.
- Neu user khong bam check-in, flow missed/escalation van chay nhu cu.

### Bo qua transition

Transition chi xuat hien khi nguoi dung doi gio check-in va lich moi tao ra khoang cach khong tot:

1. Tinh `candidate` theo gio check-in moi.
2. Neu `candidate - lastCheckInAt >= 8h`, tao `candidate`.
3. Neu `candidate` qua gan, tinh `nextCandidate` ngay mai.
4. Neu `nextCandidate - lastCheckInAt <= 28h`, tao `nextCandidate`.
5. Neu `nextCandidate` qua xa, tao transition tai `lastCheckInAt + 8h`, sau do lich chinh quay ve gio moi.

Khi co transition, backend kiem tra activity manh gan day:

- Chi xet khi user da bat `passiveActivityAssistEnabled`.
- Chi xet `APP_FOREGROUND`, `APP_INTERACTION`, `PUSH_TAPPED`.
- Activity phai xay ra sau `lastCheckInAt`, truoc `transitionTime`, va trong 4 gio truoc `transitionTime`.

Neu co activity hop le, backend khong tu dong xoa transition. Backend chi tra:

- `transitionCanBeSkipped = true`
- `transitionSkipReason`
- `lastRelevantActivityAt`
- `lastRelevantActivityType`
- `passiveActivityRecommendation = ASK_USER`

FE nen hoi user:

> Ban vua hoat dong gan day tren AfterMe. Ban co muon bo check-in chuyen tiep khong?

Neu user chon bo qua, FE goi endpoint skip transition. Backend chi cho skip khi:

- transition instance thuoc user hien tai;
- reminder la system daily check-in;
- instance dang `PENDING`;
- co recent app activity du dieu kien.

### Delay canh bao nguoi than

Truoc khi gui email/SMS cho trusted contact, backend kiem tra user co hoat dong gan day tren AfterMe khong.

Rule phase 1:

- Chi chay khi user da bat `passiveActivityAssistEnabled`.
- Chi xet tin hieu manh: `APP_FOREGROUND`, `APP_INTERACTION`, `PUSH_TAPPED`.
- Activity phai nam trong 10 phut gan nhat va sau `scheduledTime`.
- Neu co activity, backend gui push noti lan cuoi cho user va delay canh bao nguoi than 5 phut.
- Moi reminder instance chi duoc delay theo passive activity 1 lan.

Push noti lan cuoi nen noi ro:

> AfterMe thay ban vua hoat dong gan day. Vui long xac nhan an toan de tranh gui canh bao cho nguoi than.

Sau 5 phut:

- Neu user check-in, escalation dung lai nhu cu.
- Neu user van chua check-in, backend gui canh bao nguoi than nhu flow hien tai.

Ly do chon 10 phut:

- Du gan de co y nghia la user vua that su dung app.
- Khong qua dai de lam yeu safety flow.
- Phu hop voi muc dich "cho user co them mot co hoi check-in" chu khong thay the check-in.

Ly do delay 5 phut:

- Du ngan de khong lam cham canh bao khan cap qua nhieu.
- Du dai de user bam check-in neu vua thay push/in-app prompt.

## Backend

Thay doi duoc thiet ke theo huong additive:

- Khong xoa field cu.
- Khong doi endpoint cu.
- Them endpoint moi.
- Logic passive activity mac dinh tat voi user cu.
- Neu FE/mobile chua gui signal, flow cu van chay binh thuong.

### Database

Migration moi:

- `V30__add_user_activity_state.sql`

Them vao `users`:

- `passive_activity_assist_enabled BOOLEAN NOT NULL DEFAULT FALSE`

Them bang:

- `user_activity_states`

Cot chinh:

- `user_id`
- `device_id`
- `last_app_foreground_at`
- `last_app_interaction_at`
- `last_push_tapped_at`
- `last_device_unlocked_at`
- `last_device_interactive_at`
- `last_motion_at`
- `last_activity_type`
- `created_at`
- `updated_at`

### Entity/repository/service moi

Entity:

- `UserActivityState`

Enums:

- `UserActivitySignalType`
- `PassiveActivityRecommendation`

Repository:

- `UserActivityStateRepository`

Service:

- `PassiveActivityService`
- `PassiveActivityServiceImpl`

Tac dung:

- Nhan signal tu mobile.
- Cap nhat latest activity state theo user/device.
- Luu setting passive activity.
- Tra ve recent strong activity cho reminder/escalation flow.

### Endpoint moi

Record signal:

```http
POST /api/user-activity-signals
```

Body:

```json
{
  "deviceId": "android-abc",
  "signalType": "APP_FOREGROUND",
  "occurredAt": "2026-06-25T08:10:00",
  "source": "MOBILE_APP",
  "confidence": 1.0
}
```

Get current activity state:

```http
GET /api/user-activity-signals/me/state
```

Update setting:

```http
PATCH /api/user-activity-signals/settings
```

Body:

```json
{
  "passiveActivityAssistEnabled": true
}
```

Skip transition:

```http
POST /api/reminders/daily-check-in-transition/skip
```

Body:

```json
{
  "transitionInstanceId": 123
}
```

### DTO da chinh sua

`DailyCheckInTimeUpdateResponseDto` giu cac field cu:

- `dailyCheckInTime`
- `nextRegularTime`
- `transitionTime`
- `expectedMissedAt`
- `nightRisk`
- `warningMessage`

Them field moi:

- `transitionInstanceId`
- `transitionCanBeSkipped`
- `transitionSkipReason`
- `lastRelevantActivityAt`
- `lastRelevantActivityType`
- `passiveActivityRecommendation`

Vi cac field moi la optional/nullable, FE cu co the bo qua neu chua tich hop.

`TodayReminderScheduleDto` them field moi:

- `earlyCheckInStartAt`
- `smartCheckInWindowEndAt`
- `responseDeadline`
- `canCheckInNow`
- `checkInPromptReason`

`ReminderInstanceResponseDto` them field moi:

- `earlyCheckInStartAt`
- `smartCheckInWindowEndAt`
- `responseDeadline`
- `canCheckInNow`
- `checkInPromptReason`

`checkInPromptReason` co the la:

- `EARLY_ACTIVITY_WINDOW`: dang trong 30 phut truoc gio check-in, nen co the hien prompt neu user vua hoat dong.
- `SCHEDULED_WINDOW`: dang trong 60 phut sau gio check-in.
- `RESPONSE_DEADLINE_WINDOW`: da qua smart prompt window nhung van con han check-in.
- `null`: chua nen hien check-in hoac instance khong con check-in duoc.

FE tich hop theo huong khong pha API cu:

1. Goi `GET /api/reminders/instances/today`.
2. Neu instance co `canCheckInNow = true`, FE co the hien nut check-in.
3. Neu mobile vua ghi nhan `APP_FOREGROUND`, `APP_INTERACTION`, `PUSH_TAPPED`, `DEVICE_UNLOCKED` va instance dang o smart window, FE co the hien prompt noi bat hon.
4. Khi user bam xac nhan, goi endpoint response/check-in cu.
5. Neu FE chua doc cac field moi, flow cu van hoat dong.

### Safety escalation

`SafetyEscalationServiceImpl` duoc bo sung logic:

- Truoc khi gui canh bao nguoi than, goi `PassiveActivityService.findRecentStrongActivity(...)`.
- Neu co app activity trong 10 phut gan nhat, gui push final prompt.
- Set `nextRemindAt = now + 5 minutes`.
- Luu `EscalationLog` voi `NotificationType.PASSIVE_ACTIVITY_DELAY` de dam bao moi instance chi delay 1 lan.
- Sau 5 phut, neu user van chua check-in, flow gui trusted contact tiep tuc nhu cu.

### Giu API cu

Endpoint cu van giu:

```http
PUT /api/reminders/daily-check-in-time
```

Khac biet:

- Response `data` da co DTO tu truoc.
- Lan nay chi them field moi vao DTO.
- FE cu neu chi doc `success/code/message` hoac cac field cu se khong bi anh huong.

Behavior cu van giu neu:

- user chua bat `passiveActivityAssistEnabled`;
- mobile chua gui signal;
- khong co activity gan day du dieu kien.

### Android signal va quyen can thiet

`APP_FOREGROUND`

- Lay duoc tu app lifecycle khi AfterMe duoc mo/dua len foreground.
- Khong can quyen Android dac biet.
- Nen luu local truoc, sau do gui `POST /api/user-activity-signals` khi co mang.

`APP_INTERACTION`

- Lay duoc khi user bam nut, scroll, mo man hinh, tuong tac voi AfterMe.
- Khong can quyen Android dac biet.
- Day la tin hieu manh nhat vi user that su dang dung app.

`PUSH_TAPPED`

- Lay duoc khi user bam vao notification cua AfterMe.
- Khong can quyen rieng cho viec ghi nhan tap, nhung app can quyen notification theo phien ban Android neu muon hien push.
- Tren Android 13+ can xin `POST_NOTIFICATIONS` de hien notification.

`DEVICE_UNLOCKED`

- Co the nghe su kien `Intent.ACTION_USER_PRESENT` khi user vua mo khoa dien thoai.
- Thuong khong can dangerous runtime permission.
- Tuy nhien Android gioi han background broadcast: nen dang ky receiver luc app dang chay/foreground service, khong nen coi la tin hieu dam bao luon nhan duoc khi app bi kill.
- Khong nen tu dong check-in bang signal nay, chi nen dung de hien local notification/prompt.

`DEVICE_INTERACTIVE` hoac screen on/off

- Co the theo doi `Intent.ACTION_SCREEN_ON`, `Intent.ACTION_SCREEN_OFF`, hoac PowerManager khi app dang chay.
- Khong can dangerous runtime permission.
- Do tin cay thap vi man hinh co the sang do thong bao/he thong, khong chac user dang an toan.
- Chi nen dung lam weak signal de delay/prompt nhe, khong dung lam dieu kien bo qua check-in.

`UsageStatsManager`

- Neu muon biet user da dung dien thoai/app nao trong bao lau, Android yeu cau quyen dac biet `PACKAGE_USAGE_STATS`.
- Quyen nay khong xin bang runtime permission thong thuong; user phai bat trong Settings > Usage Access.
- Day la quyen nhay cam, nen chi nen la optional advanced feature, giai thich ro muc dich va khong bat buoc cho flow chinh.

Huong trien khai hop ly:

- Phase 1: dung `APP_FOREGROUND`, `APP_INTERACTION`, `PUSH_TAPPED`. It rui ro, khong can quyen nhay cam.
- Phase 2: them `DEVICE_UNLOCKED` de hien prompt linh hoat hon gan gio check-in.
- Phase 3: neu that su can, them Usage Access cho user opt-in, nhung khong dua vao day de ket luan user an toan.
