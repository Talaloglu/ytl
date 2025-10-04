# 🧪 Phase 1 Testing Guide

## Complete verification checklist for all Phase 1 components

---

## 1️⃣ Build Verification

### **Step 1: Clean Build**
```bash
cd c:\Users\ALIA\Desktop\movieApp
./gradlew clean
```

### **Step 2: Compile Check**
```bash
./gradlew compileDebugKotlin
```

**Expected:** ✅ BUILD SUCCESSFUL

### **Step 3: Full Debug Build**
```bash
./gradlew assembleDebug
```

**Expected:** ✅ APK generated at `app/build/outputs/apk/debug/app-debug.apk`

---

## 2️⃣ Database Testing

### **Test 2.1: Database Schema**
**File:** `app/schemas/com.movieapp.data.local.AppDatabase/4.json`

**Check:**
- [ ] Schema file exists for version 4
- [ ] Contains all 5 tables
- [ ] Auto-migration specs present

### **Test 2.2: Entity Compilation**
**Run:**
```bash
./gradlew :app:kaptDebugKotlin
```

**Expected:** 
- [ ] No compilation errors
- [ ] DAO implementations generated
- [ ] Database implementation generated

### **Test 2.3: Room Validation**
Check generated files:
- `app/build/generated/source/kapt/debug/.../AppDatabase_Impl.java`
- `app/build/generated/source/kapt/debug/.../UserProfileDao_Impl.java`
- `app/build/generated/source/kapt/debug/.../WatchlistCacheDao_Impl.java`
- `app/build/generated/source/kapt/debug/.../WatchProgressCacheDao_Impl.java`
- `app/build/generated/source/kapt/debug/.../ViewingHistoryCacheDao_Impl.java`

---

## 3️⃣ Application Classes Testing

### **Test 3.1: MovieAppApplication**
**Location:** `app/src/main/java/com/movieapp/MovieAppApplication.kt`

**Verification:**
```kotlin
// Check imports compile
import coil.ImageLoader
import coil.ImageLoaderFactory
import leakcanary.LeakCanary
```

**Manual Test (after install):**
1. Install debug APK
2. Open app
3. Check logcat for: `🚀 MovieAppApplication: Initialized`
4. Check logcat for: `🔍 LeakCanary: Configured for memory leak detection`

### **Test 3.2: OptimizedMovieAppApplication**
**Location:** `app/src/main/java/com/movieapp/OptimizedMovieAppApplication.kt`

**Manual Test:**
1. Check logcat for: `🚀 OptimizedMovieApp: Initializing application`
2. Check logcat for: `✅ Database initialized`
3. Check logcat for: `✅ OptimizedMovieApp: Application initialized successfully`

### **Test 3.3: AndroidManifest**
**Verify:**
```xml
<application android:name=".MovieAppApplication" ...>
```

**Check:**
- [ ] Application class registered
- [ ] No manifest merge errors

---

## 4️⃣ Database Runtime Testing

### **Test 4.1: Database Creation**
**After installing app:**

```bash
# Check database exists
adb shell run-as com.movieapp ls /data/data/com.movieapp/databases/

# Expected output:
# movie_app.db
# movie_app.db-shm
# movie_app.db-wal
```

### **Test 4.2: Database Version**
```bash
# Check version
adb shell run-as com.movieapp cat /data/data/com.movieapp/databases/movie_app.db-journal
```

**Or use Android Studio Database Inspector:**
1. Tools → Database Inspector
2. Connect to running app
3. Verify tables exist:
   - cached_movie
   - user_profile
   - watchlist_cache
   - watch_progress_cache
   - viewing_history_cache

### **Test 4.3: Table Structures**
**Check each table has correct columns:**

**user_profile:**
- userId (TEXT, PRIMARY KEY)
- email (TEXT)
- displayName (TEXT)
- avatarUrl (TEXT)
- createdAt (INTEGER)
- updatedAt (INTEGER)

**watchlist_cache:**
- userId (TEXT)
- movieId (INTEGER)
- addedAt (INTEGER)
- syncedAt (INTEGER)
- needsSync (INTEGER/BOOLEAN)
- PRIMARY KEY (userId, movieId)

**watch_progress_cache:**
- userId (TEXT)
- movieId (INTEGER)
- currentPositionMs (INTEGER)
- durationMs (INTEGER)
- watchPercentage (REAL)
- lastUpdatedAt (INTEGER)
- isCompleted (INTEGER/BOOLEAN)
- syncedAt (INTEGER)
- needsSync (INTEGER/BOOLEAN)
- PRIMARY KEY (userId, movieId)

**viewing_history_cache:**
- id (INTEGER, PRIMARY KEY, AUTOINCREMENT)
- userId (TEXT)
- movieId (INTEGER)
- viewedAt (INTEGER)
- viewDurationMs (INTEGER)
- completedView (INTEGER/BOOLEAN)
- syncedAt (INTEGER)
- needsSync (INTEGER/BOOLEAN)

---

## 5️⃣ Authentication Testing

### **Test 5.1: AuthState Compilation**
```bash
# Verify AuthState.kt compiles
./gradlew :app:compileDebugKotlin --info | grep AuthState
```

**Expected:** No errors

### **Test 5.2: AuthRepository Compilation**
```bash
./gradlew :app:compileDebugKotlin --info | grep AuthRepository
```

**Expected:** No errors

### **Test 5.3: Supabase GoTrue Dependency**
**Check build.gradle contains:**
```gradle
implementation 'io.github.jan-tennert.supabase:gotrue-kt:2.0.4'
```

### **Test 5.4: Runtime Auth Test**
**Manual test after install:**

1. Create a simple test button in UI (or use existing auth screen)
2. Try signup with test credentials
3. Check logcat for:
   - `AuthRepository: Signing up user: test@example.com`
   - `AuthRepository: Sign up successful for user: <uuid>`
   - `UserProfileRepository: Created profile for user: <uuid>`

**Verification queries:**
```bash
# Check if profile was created
adb shell run-as com.movieapp sqlite3 /data/data/com.movieapp/databases/movie_app.db \
  "SELECT * FROM user_profile;"
```

---

## 6️⃣ Image Caching Testing

### **Test 6.1: Coil Integration**
**Check logcat after loading images:**
```
# Expected in debug builds:
[Coil] Loaded image from network
[Coil] Cached to disk
[Coil] Cached to memory
```

### **Test 6.2: Cache Directory**
```bash
# Check image cache exists
adb shell run-as com.movieapp ls /data/data/com.movieapp/cache/

# Expected:
# image_cache/
```

### **Test 6.3: Cache Size Limits**
**Verify:**
- Memory cache: ~25% of device RAM
- Disk cache: Max 250MB

```bash
# Check cache size
adb shell run-as com.movieapp du -sh /data/data/com.movieapp/cache/image_cache/
```

---

## 7️⃣ Memory Leak Testing

### **Test 7.1: LeakCanary Installation**
**Debug build only:**
```bash
# Check LeakCanary is installed
adb shell pm list packages | grep leakcanary
```

**Expected output:**
```
package:com.squareup.leakcanary
```

### **Test 7.2: LeakCanary Notification**
**Manual test:**
1. Install debug APK
2. Navigate through app screens
3. Trigger memory pressure (open/close activities multiple times)
4. Check for LeakCanary notifications (if any leaks detected)

**Expected:** No leak notifications

---

## 8️⃣ Memory Management Testing

### **Test 8.1: onLowMemory Trigger**
**Simulate low memory:**
```bash
# Force low memory condition
adb shell am send-trim-memory com.movieapp RUNNING_CRITICAL
```

**Check logcat for:**
```
OptimizedMovieApp: 🚨 Memory trim level: RUNNING_CRITICAL - emergency cleanup
OptimizedMovieApp: ✅ Non-essential data cleared
```

### **Test 8.2: Memory Trim Levels**
**Test each level:**
```bash
# Moderate memory pressure
adb shell am send-trim-memory com.movieapp RUNNING_MODERATE

# Low memory
adb shell am send-trim-memory com.movieapp RUNNING_LOW

# Background
adb shell am send-trim-memory com.movieapp BACKGROUND
```

**Expected:** Appropriate cleanup logged for each level

---

## 9️⃣ Migration Testing

### **Test 9.1: Clean Install (v4)**
1. Uninstall app completely
2. Install fresh debug build
3. Open app
4. Check database version = 4

**Verification:**
```bash
adb shell run-as com.movieapp sqlite3 /data/data/com.movieapp/databases/movie_app.db \
  "PRAGMA user_version;"
```

**Expected:** `4`

### **Test 9.2: Migration from v1 (if possible)**
**If you have v1 backup:**
1. Install v1 APK
2. Create some data
3. Install current debug build (v4)
4. Check data preserved
5. Check new tables created

**Verification:**
```bash
# Check all tables exist
adb shell run-as com.movieapp sqlite3 /data/data/com.movieapp/databases/movie_app.db \
  ".tables"
```

**Expected:**
```
cached_movie
user_profile
viewing_history_cache
watch_progress_cache
watchlist_cache
```

---

## 🔟 Integration Testing

### **Test 10.1: Full Auth Flow**
**Manual test:**
1. Launch app
2. Sign up with email/password
3. Verify profile created in database
4. Sign out
5. Sign in with same credentials
6. Verify session restored

**Database verification after each step:**
```bash
adb shell run-as com.movieapp sqlite3 /data/data/com.movieapp/databases/movie_app.db \
  "SELECT userId, email, displayName FROM user_profile;"
```

### **Test 10.2: Profile Creation Flow**
**Verify:**
1. Signup creates profile automatically
2. Signin fetches existing profile
3. Profile contains correct email
4. Timestamps are set correctly

### **Test 10.3: Session Persistence**
**Test:**
1. Sign in
2. Close app completely
3. Reopen app
4. Verify still signed in
5. Check authState = Authenticated

---

## 📊 Performance Testing

### **Test 11.1: App Startup Time**
**Measure:**
```bash
adb shell am start -W com.movieapp/.MainActivity
```

**Expected metrics:**
- TotalTime: < 2000ms (2 seconds)
- WaitTime: < 2500ms

### **Test 11.2: Database Query Performance**
**Use Database Inspector profiler:**
1. Profile → Database queries
2. Check query execution times
3. Verify indexes are used

**Expected:**
- Simple queries: < 10ms
- Complex joins: < 50ms

### **Test 11.3: Memory Usage**
**Monitor with Android Profiler:**
1. Open Memory Profiler
2. Track memory over time
3. Verify no continuous growth
4. Check GC frequency

**Expected:**
- Memory usage stable
- No continuous growth
- GC every 30-60 seconds (normal)

---

## ✅ Verification Checklist

### **Build Checks:**
- [ ] Clean build succeeds
- [ ] Debug APK generated
- [ ] No compilation errors
- [ ] No lint errors

### **Database Checks:**
- [ ] Database created with version 4
- [ ] All 5 tables exist
- [ ] All columns correct
- [ ] Indexes created
- [ ] Auto-migrations work

### **Application Checks:**
- [ ] MovieAppApplication initializes
- [ ] OptimizedMovieAppApplication initializes
- [ ] Database initialized on startup
- [ ] No crashes on launch

### **Auth Checks:**
- [ ] Can sign up new user
- [ ] Profile created automatically
- [ ] Can sign in existing user
- [ ] Can sign out
- [ ] Session persists
- [ ] AuthState updates correctly

### **Memory Checks:**
- [ ] Image cache working
- [ ] LeakCanary installed (debug)
- [ ] Memory trimming responds
- [ ] No memory leaks detected
- [ ] Cache limits respected

### **Performance Checks:**
- [ ] App starts in < 2 seconds
- [ ] Database queries fast
- [ ] No ANRs
- [ ] Smooth UI performance

---

## 🐛 Known Issues to Check

### **Potential Issues:**
1. **Supabase not configured** → Check local.properties has keys
2. **Database migration fails** → Check schema compatibility
3. **Auth fails** → Check internet connection
4. **LeakCanary not showing** → Only in debug builds
5. **Cache not working** → Check permissions

### **Error Logs to Watch:**
```
E/AndroidRuntime: FATAL EXCEPTION
E/SQLiteDatabase: Error
E/Supabase: Connection failed
E/AuthRepository: Sign up failed
E/DatabaseProvider: Database initialization failed
```

---

## 📝 Test Results Template

```
## Phase 1 Test Results

**Date:** [Date]
**Build:** Debug
**Device:** [Device Name]
**Android Version:** [Version]

### Build Tests:
- Clean build: ✅/❌
- APK generated: ✅/❌

### Database Tests:
- Database v4 created: ✅/❌
- All tables exist: ✅/❌
- Migrations work: ✅/❌

### Auth Tests:
- Sign up works: ✅/❌
- Sign in works: ✅/❌
- Profile created: ✅/❌
- Session persists: ✅/❌

### Memory Tests:
- Image cache works: ✅/❌
- LeakCanary active: ✅/❌
- Memory trimming works: ✅/❌

### Performance Tests:
- Startup time: [ms]
- Memory usage: [MB]
- No ANRs: ✅/❌

### Issues Found:
[List any issues]

### Overall Result: ✅/❌
```

---

## 🚀 Next Steps After Testing

**If all tests pass:**
→ Proceed to Phase 2: User Features

**If tests fail:**
→ Fix issues before proceeding
→ Document problems
→ Re-test after fixes

**Critical failures (cannot proceed):**
- Database not created
- App crashes on launch
- Auth completely broken

**Non-critical failures (can fix later):**
- Memory leaks detected
- Slow performance
- Minor UI issues
