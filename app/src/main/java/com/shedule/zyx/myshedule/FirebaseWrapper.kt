package com.shedule.zyx.myshedule

import app.voter.xyz.RxFirebase
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.crash.FirebaseCrash
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.shedule.zyx.myshedule.config.AppPreference
import com.shedule.zyx.myshedule.models.Group
import com.shedule.zyx.myshedule.models.Schedule
import com.shedule.zyx.myshedule.models.Teacher
import com.shedule.zyx.myshedule.utils.Constants.Companion.ADMINS
import com.shedule.zyx.myshedule.utils.Constants.Companion.CHANGES_COUNT
import com.shedule.zyx.myshedule.utils.Constants.Companion.RATINGS
import com.shedule.zyx.myshedule.utils.Constants.Companion.SCHEDULE
import com.shedule.zyx.myshedule.utils.Utils.Companion.getKeyByName
import rx.Observable
import java.util.*

/**
 * Created by bogdan on 16.09.16.
 */
class FirebaseWrapper(val ref: DatabaseReference, val prefs: AppPreference, val auth: FirebaseAuth) {

  fun createTeacherRef() = facultyRef().child("teachers")

  private fun facultyRef(): DatabaseReference {
    return ref.child(getKeyByName(prefs.getUniverName() ?: ""))
        .child(getKeyByName(prefs.getFacultyName() ?: ""))
  }

  private fun groupRef(): DatabaseReference {
    return ref.child(getKeyByName(prefs.getUniverName() ?: ""))
        .child(getKeyByName(prefs.getFacultyName() ?: ""))
        .child(getKeyByName(prefs.getGroupName() ?: ""))
  }

  fun createAccount(): Observable<Void> {
    return Observable.create {
      auth.signInAnonymously().addOnCompleteListener { task ->
        if (task.isSuccessful) {
          it.onNext(null)
          it.onCompleted()
        } else it.onError(task.exception)
      }
    }
  }

  fun logOut(): Observable<Void> {
    return Observable.create {
      auth.currentUser?.delete()?.addOnCompleteListener { task ->
        if (task.isSuccessful) {
          it.onNext(null)
          it.onCompleted()
        } else it.onError(task.exception)
      }
    }
  }

  fun getGroups(faculty: String, university: String): Observable<List<String>?> {
    return RxFirebase.observe(ref.child(university).child(faculty)).map { it.children?.map { it.key }?.filter { it != "teachers" } }
  }

  fun getFaculty(university: String): Observable<List<String>?> {
    return RxFirebase.observe(ref.child(university)).map { it.children?.map { it.key } }
  }

  fun getUniversity(): Observable<List<String>?> {
    return RxFirebase.observe(ref).map { it.children?.map { it.key } }
  }

  fun getSubjects() = getTeachers().map { it?.map { it.lessonName.toString() } }

  fun getTeachers(): Observable<List<Teacher>?> {
    return RxFirebase.observe(createTeacherRef())
        .filter { it != null }
        .filter { it.value != null }
        .map { it.children.map { it.getValue(Teacher::class.java) } }
  }

  fun getSchedule(): Observable<List<Schedule>?> {
    return RxFirebase.observe(groupRef().child(SCHEDULE))
        .filter { it != null }
        .filter { it.value != null }.map { it.children.map { it.getValue(Schedule::class.java) } }
  }

  fun pushSchedule(list: List<Schedule>, change: Int): Observable<Boolean> {
    groupRef().child(SCHEDULE).setValue(list)
    groupRef().child(CHANGES_COUNT).setValue(change)
    return RxFirebase.observe(groupRef())
        .flatMap { if (getSchedule() != null) Observable.just(true) else Observable.just(false) }
  }

  fun getAdmins(): Observable<List<String>?> {
    return RxFirebase.observe(groupRef().child(ADMINS))
        .filter { it != null }
        .filter { it.value != null }.map { it.children.map { it.getValue(String::class.java) } }
  }

  fun pushAdmin(): Observable<Boolean> {
    groupRef().child(ADMINS).setValue(auth.currentUser?.uid)
    return RxFirebase.observe(groupRef().child(ADMINS))
        .flatMap { if (getAdmins() != null) Observable.just(true) else Observable.just(false) }
  }

  fun pushChangesCount(count: Int): Observable<Boolean> {
    groupRef().child(CHANGES_COUNT).setValue(count)
    return RxFirebase.observe(groupRef().child(CHANGES_COUNT))
        .flatMap { Observable.just(true) }
  }

  fun getChangesCount(): Observable<Int> {
    return RxFirebase.observe(groupRef().child(CHANGES_COUNT))
        .map { it.getValue(Int::class.java) }
  }

  fun getGroupInformation() = RxFirebase.observe(groupRef()).map { it.getValue(Group::class.java) }

  fun pushGroup(): Observable<Boolean> {
    groupRef().child(CHANGES_COUNT).setValue(0)
    groupRef().child(ADMINS).push().setValue(auth.currentUser?.uid)
    return RxFirebase.observe(groupRef())
        .flatMap { if (getGroupInformation() != null ) Observable.just(true) else Observable.just(false) }
  }

  fun pushTeacher(teachers: List<Teacher>): Observable<Boolean> {
    var loaded = false

    return Observable.create { subscriber ->
      teachers.map { teacher ->
        createTeacherRef().child(getKeyByName(teacher.teacherName)).addListenerForSingleValueEvent(object : ValueEventListener {
          override fun onCancelled(p0: DatabaseError?) {
          }

          override fun onDataChange(data: DataSnapshot?) {
            loaded = true
            if (data?.value == null) {
              createTeacherRef().child(getKeyByName(teacher.teacherName)).setValue(teacher)
              teacherCallback()
            } else {
              (data?.value as HashMap<String, Any>).keys.toList()
                  .filter { it.equals(getKeyByName(teacher.teacherName)) }
                  .firstOrNull()?.let {
                createTeacherRef().child(it)
                    .updateChildren(mapOf(Pair("", ObjectMapper().convertValue(teacher, Map::class.java))))
              }

              teacherCallback()
            }
          }

          private fun teacherCallback() {
            getTeachers().subscribe({
              if (it == null) {
                subscriber.onNext(false)
                subscriber.onCompleted()
              } else {
                RxFirebase.observeChildAdded(createTeacherRef())
                    .subscribe({
                      subscriber.onNext(true)
                      subscriber.onCompleted()
                    }, {
                      if (BuildConfig.DEBOUG_ENABLED)
                        FirebaseCrash.report(it)
                      subscriber.onError(it)
                    })
              }
            }, {
              if (BuildConfig.DEBOUG_ENABLED)
                FirebaseCrash.report(it)
              subscriber.onError(it)
            })
          }
        })
      }

      getTeachers().subscribe({
        if (it == null) {
          if (!loaded) {
            subscriber.onNext(false)
            subscriber.onCompleted()
          }
        } else {
          RxFirebase.observeChildAdded(createTeacherRef())
              .subscribe({
                if (!loaded) {
                  subscriber.onNext(true)
                  subscriber.onCompleted()
                }
              }, {
                if (BuildConfig.DEBOUG_ENABLED)
                  FirebaseCrash.report(it)
                subscriber.onError(it)
              })
        }
      }, {
        if (BuildConfig.DEBOUG_ENABLED)
          FirebaseCrash.report(it)
        subscriber.onError(it)
      })
    }
  }

  fun pushRating(rating: Double, teacherName: String) {
    createTeacherRef().child(getKeyByName(teacherName)).child(RATINGS).child(auth.currentUser?.uid).setValue(rating)
  }
}