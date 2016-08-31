package com.shedule.zyx.myshedule.ui.activities

import android.app.Activity
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.support.design.widget.BottomSheetBehavior
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import com.prolificinteractive.materialcalendarview.CalendarDay
import com.prolificinteractive.materialcalendarview.MaterialCalendarView
import com.shedule.zyx.myshedule.R
import com.shedule.zyx.myshedule.R.layout.add_schedule_activity
import com.shedule.zyx.myshedule.ScheduleApplication
import com.shedule.zyx.myshedule.managers.DateManager
import com.shedule.zyx.myshedule.managers.ScheduleManager
import com.shedule.zyx.myshedule.models.*
import com.shedule.zyx.myshedule.models.Date
import com.wdullaer.materialdatetimepicker.date.DatePickerDialog
import com.wdullaer.materialdatetimepicker.time.RadialPickerLayout
import com.wdullaer.materialdatetimepicker.time.TimePickerDialog
import kotlinx.android.synthetic.main.add_item_view.*
import kotlinx.android.synthetic.main.add_schedule_activity.*
import kotlinx.android.synthetic.main.number_layout.*
import org.jetbrains.anko.*
import java.util.*
import javax.inject.Inject

/**
 * Created by alexkowlew on 26.08.2016.
 */
class AddScheduleActivity : AppCompatActivity(), TimePickerDialog.OnTimeSetListener, DatePickerDialog.OnDateSetListener {

  @Inject
  lateinit var scheduleManager: ScheduleManager

  @Inject
  lateinit var dateManager: DateManager

  var switcher = 0
  var startPeriod: Date? = null
  var endPeriod: Date? = null
  var startTime: Time? = null
  var endTime: Time? = null
  var category: Category? = null


  var listOfDates = arrayListOf<CalendarDay>()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(add_schedule_activity)
    ScheduleApplication.getComponent().inject(this)
    setSupportActionBar(add_schedule_toolbar)
    add_schedule_toolbar.title = applicationContext.getString(R.string.add_schedule_toolbar_title)
    add_schedule_toolbar.setTitleTextColor(Color.WHITE)

    number_of_lesson.onClick {
      val bottomSheetBehavior = BottomSheetBehavior.from(bottom_sheet)
      bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
    }

    start_period_of_lesson.onClick { showDateDialog() }

    end_period_of_lesson.onClick {
      showDateDialog()
      switcher = 1
    }

    start_time_of_lesson.onClick { showTimeDialog() }

    end_time_of_lesson.onClick {
      switcher = 1
      showTimeDialog()
    }
    category = Category.HOME_EXAM
    exam.onTouch { view, motionEvent -> setColor(Category.EXAM, resources.getColor(R.color.mark_red)); false }
    course_work.onClick { setColor(Category.COURSE_WORK, resources.getColor(R.color.mark_orange)) }
    standings.onClick { setColor(Category.STANDINGS, resources.getColor(R.color.mark_yellow)) }
    home_exam.onClick { setColor(Category.HOME_EXAM, resources.getColor(R.color.dark_cyan)) }

    spinner_number_of_lesson.onItemSelectedListener {
      onItemSelected { adapterView, view, i, l ->
        number_of_lesson.text = "${i + 1}"
      }
    }

    categoriesColors()
  }

  private fun setColor(cat: Category, color: Int) {
    (number_of_lesson.background as GradientDrawable).setColor(color)
    category = cat
  }

  fun categoriesColors() {
    (exam.background as GradientDrawable).setColor(resources.getColor(R.color.mark_red))
    (course_work.background as GradientDrawable).setColor(resources.getColor(R.color.mark_orange))
    (standings.background as GradientDrawable).setColor(resources.getColor(R.color.mark_yellow))
    (home_exam.background as GradientDrawable).setColor(resources.getColor(R.color.dark_cyan))
  }

  override fun onOptionsItemSelected(item: MenuItem?): Boolean {
    when (item?.itemId) {
      R.id.add_item -> {
        parseDataForSchedule(); return true
      }
      R.id.additional_calendar -> {
        alert {
          customView {
            include<View>(R.layout.calendar_layout) {
              val calendarView = find<MaterialCalendarView>(R.id.calendarView)
              calendarView.selectionMode = MaterialCalendarView.SELECTION_MODE_MULTIPLE
              find<TextView>(R.id.clear_dates).onClick { calendarView.clearSelection() }
              find<TextView>(R.id.cancel_dates).onClick { dismiss() }
              find<TextView>(R.id.approve_dates).onClick {
                listOfDates.addAll(calendarView.selectedDates)
                dismiss()
              }
            }
          }
        }.show(); return true
      }
      else -> return super.onOptionsItemSelected(item)
    }
  }

  override fun onCreateOptionsMenu(menu: Menu?): Boolean {
    menuInflater.inflate(R.menu.add_schedule_item_menu, menu)
    return super.onCreateOptionsMenu(menu)
  }

  private fun parseDataForSchedule() {
    startPeriod?.let { start ->
      endPeriod?.let { end ->
        val schedule = Schedule(number_of_lesson.text.toString(),
            if (name_of_lesson.text.toString().isEmpty()) "" else name_of_lesson.text.toString(),
            start, end)

        schedule.location = Location(classroom.text.toString(), housing.text.toString())
        schedule.startTime = startTime
        schedule.endTime = endTime
        schedule.teacher = name_of_teacher.text.toString()
        schedule.typeLesson = if (spinner_type_of_lesson.selectedItem.toString().equals("Практика")) TypeLesson.SEMINAR else TypeLesson.LECTURE
        schedule.category = category
        schedule.dates.addAll(scheduleManager.getScheduleByDate(schedule.startPeriod, schedule.endPeriod,
            intent.getIntExtra("current_day_of_week", 0)).map { it })
        scheduleManager.globalList.add(schedule)
        setResult(Activity.RESULT_OK)
        finish()
      }
//      toast("Упс! Введите окончание периода предмета ")
    }

//    toast("Упс! Введите начало периода предмета")
  }

  private fun showTimeDialog() {
    val now = Calendar.getInstance()
    val dialog = TimePickerDialog.newInstance(
        this@AddScheduleActivity,
        now.get(Calendar.HOUR_OF_DAY),
        now.get(Calendar.MINUTE),
        true)
    dialog.accentColor = Color.RED
    dialog.show(fragmentManager, "")
  }

  private fun showDateDialog() {
    val now = Calendar.getInstance()
    val dialog = DatePickerDialog.newInstance(
        this@AddScheduleActivity,
        now.get(Calendar.YEAR),
        now.get(Calendar.MONTH),
        now.get(Calendar.DAY_OF_MONTH))
    dialog.accentColor = Color.BLUE
    dialog.show(fragmentManager, "")
  }

  override fun onTimeSet(view: RadialPickerLayout?, hourOfDay: Int, minute: Int, second: Int) {
    val h = if (hourOfDay < 10) "0$hourOfDay" else "$hourOfDay"
    val m = if (minute < 10) "0$minute" else "$minute"
    val time = "$h:$m"
    when (switcher) {
      1 -> {
        end_time_of_lesson.text = time
        switcher = 0
        endTime = Time(hourOfDay, minute)
      }
      else -> {
        start_time_of_lesson.text = time
        startTime = Time(hourOfDay, minute)
      }
    }
  }

  override fun onDateSet(view: DatePickerDialog?, year: Int, monthOfYear: Int, dayOfMonth: Int) {
    val day = if (dayOfMonth < 10) "0$dayOfMonth" else "$dayOfMonth"
    val month = if (monthOfYear < 10) "0${monthOfYear + 1}" else "${monthOfYear + 1}"
    val date = "$day.$month.$year"
    when (switcher) {
      1 -> {
        end_period_of_lesson.text = date
        switcher = 0
        endPeriod = Date(dayOfMonth, monthOfYear, year)
      }
      else -> {
        start_period_of_lesson.text = date
        startPeriod = Date(dayOfMonth, monthOfYear, year)
      }
    }
  }

}