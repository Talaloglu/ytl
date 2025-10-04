# 🧹 Codebase Cleanup Plan

## Analysis Summary

I've analyzed your current codebase and identified redundant, unused, and deprecated code that should be cleaned up before implementing Phase 1.

---

## 🗑️ Files to REMOVE

### 1. **Duplicate/Unused Screens**

#### ❌ `HomeScreen.kt` - REMOVE
**Reason:** Superseded by `CategorizedHomeScreen.kt` and not used in navigation
- Uses old `MainViewModel` 
- Basic grid layout without streaming support
- Not referenced in `MovieAppNavigation.kt`

**Action:** DELETE

#### ❌ `StreamingHomeScreen.kt` - KEEP BUT WILL BE REPLACED
**Reason:** Currently used but will be replaced by `OptimizedHomeScreen.kt` in Phase 4
- Keep for now as it's in active navigation
- Mark for replacement in Phase 4

#### ❌ `MovieDetailScreen.kt` - REMOVE
**Reason:** Duplicate of `DetailsScreen.kt`
- Both screens show movie details
- `DetailsScreen.kt` is more complete and used in navigation
- `MovieDetailScreen.kt` not referenced

**Action:** DELETE

#### ❌ `MovieListScreen.kt` - REMOVE
**Reason:** Functionality covered by `CategoryDetailScreen.kt`
- Simple list implementation
- Not used in navigation
- `CategoryDetailScreen.kt` is more feature-rich

**Action:** DELETE

---

### 2. **Unused Repositories**

#### ⚠️ `MovieRepository.kt` - MARK AS DEPRECATED
**Reason:** TMDB operations disabled, functionality moved to `CombinedMovieRepository.kt`
```kotlin
private val apiKey = "TMDB_DISABLED_IN_SUPABASE_FIRST_MODE"
```
- All methods throw `UnsupportedOperationException`
- `CombinedMovieRepository` handles all movie operations now

**Action:** Add deprecation notice, keep temporarily for reference

#### ⚠️ `SupabaseAuthRepository.kt` - CHECK USAGE
**Reason:** Will be replaced by new `AuthRepository.kt` in Phase 1

**Action:** Analyze usage first, then deprecate

---

### 3. **Unused API Interfaces**

#### ⚠️ `MovieApiService.kt` - CONSOLIDATE
**Reason:** Duplicate of `ApiInterface.kt`
- Both define same TMDB endpoints
- `ApiInterface.kt` is used by repositories

**Action:** Remove and use `ApiInterface.kt` exclusively

---

### 4. **Unused ViewModels**

#### ⚠️ `MainViewModel.kt` - CHECK USAGE
**Reason:** Only used by deprecated `HomeScreen.kt`
- If `HomeScreen.kt` is removed, this can be removed too

**Action:** Remove after confirming `HomeScreen.kt` removal

---

### 5. **Redundant Utilities**

#### ⚠️ `RetrofitInstance.kt` - CONSOLIDATE
**Reason:** Functionality duplicated in API package
- `data/api/RetrofitInstance.kt` exists
- `utils/RetrofitInstance.kt` exists
- Same functionality in both

**Action:** Keep only one (prefer `data/api/RetrofitInstance.kt`)

---

### 6. **Unused Models**

#### ⚠️ `Metadata.kt` - CHECK USAGE
**Reason:** May not be used anywhere

**Action:** Search for usage, remove if unused

#### ⚠️ `MoviesList.kt` - CHECK USAGE
**Reason:** May be duplicate of `MovieResponse.kt`

**Action:** Analyze and consolidate

#### ⚠️ `SupabaseEnrichedMovie.kt` - VERIFY USAGE
**Reason:** Check if still used or replaced by `CombinedMovie`

**Action:** Search for usage, remove if redundant

---

### 7. **Documentation Files to Archive**

These are helpful but can be moved to a `/docs` folder:

- `COMPILATION_FIXES.md`
- `COMPILATION_FIXES_SUMMARY.md`
- `ENHANCED_MATCHING_INSTRUCTIONS.txt`
- `EXOPLAYER_INTEGRATION.md`
- `FINAL_VALIDATION_SUMMARY.md`
- `OPTIMIZATIONS.md`
- `OPTIMIZED_SUPABASE_INTEGRATION.md`
- `PROJECT_VALIDATION_REPORT.md`
- `STEP_2_IMPLEMENTATION_SUMMARY.md`
- `STEP_3_IMPLEMENTATION_SUMMARY.md`
- `STREAMING_VERIFICATION_GUIDE.md`
- `STRUCTURAL_VALIDATION_DETAILED.md`
- `TMDB_SUPABASE_MATCHING_VALIDATION.md`
- `VIDEO_PLAYER_403_FIX.md`

**Action:** Create `/docs` folder and move all documentation there

---

## 📝 Cleanup Execution Plan

### Step 1: Create Documentation Folder
```bash
mkdir docs
mv *.md docs/ (except README.md, QUICK_SETUP.md, SUPABASE_SETUP.md)
mv *.txt docs/
```

### Step 2: Verify Usage Before Deletion
Run searches to confirm these files aren't imported anywhere:
- `HomeScreen.kt`
- `MovieDetailScreen.kt`
- `MovieListScreen.kt`
- `MainViewModel.kt`
- `MovieApiService.kt`

### Step 3: Remove Unused Files
Delete confirmed unused files

### Step 4: Add Deprecation Notices
Add `@Deprecated` annotations to files being phased out:
```kotlin
@Deprecated(
    message = "Use CombinedMovieRepository instead",
    replaceWith = ReplaceWith("CombinedMovieRepository"),
    level = DeprecationLevel.WARNING
)
class MovieRepository { ... }
```

### Step 5: Update Navigation
Ensure `MovieAppNavigation.kt` doesn't reference deleted screens

### Step 6: Clean Build Files
```bash
./gradlew clean
rm -rf .gradle/
rm -rf build/
rm -rf app/build/
```

---

## 🎯 Expected Results After Cleanup

### Files Removed: ~8 files
- `HomeScreen.kt`
- `MovieDetailScreen.kt` 
- `MovieListScreen.kt`
- `MainViewModel.kt` (if not used)
- `MovieApiService.kt`
- `utils/RetrofitInstance.kt` (duplicate)
- Unused models (TBD)

### Files Deprecated: ~3 files
- `MovieRepository.kt`
- `SupabaseAuthRepository.kt`

### Documentation Organized:
- All `.md` and `.txt` files moved to `/docs` folder
- Keep only essential docs in root

### Estimated Space Saved:
- ~2000+ lines of dead code removed
- ~15 documentation files organized
- Cleaner project structure

---

## ⚠️ Safety Checks

Before deletion, verify:
1. ✅ File is not imported anywhere
2. ✅ File is not in navigation
3. ✅ File is not referenced in ViewModel
4. ✅ File has no active TODO items
5. ✅ Backup project first (git commit)

---

## 🚀 Ready to Execute?

Would you like me to:
1. **Run automated checks** to verify safe deletions?
2. **Execute cleanup step-by-step** with confirmation?
3. **Create git commit** before cleanup?
4. **Start immediately** with the cleanup?
