package com.shedule.zyx.myshedule.widget

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import com.shedule.zyx.myshedule.R
import com.shedule.zyx.myshedule.models.Teacher
import com.shedule.zyx.myshedule.utils.Utils
import kotlinx.android.synthetic.main.teacher_view.view.*
import org.jetbrains.anko.selector

/**
 * Created by alexkowlew on 06.09.2016.
 */
class TeacherView : FrameLayout, View.OnClickListener {

  constructor(context: Context?) : super(context) {
    init(context)
  }

  constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
    init(context)
  }

  constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
    init(context)
  }

  constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes) {
    init(context)
  }

  fun init(context: Context?) {
    inflate(context, R.layout.teacher_view, this)
    tv_assessment_of_teacher.setOnClickListener(this)
    setAssessment("E")
  }

  fun setData(teacher: Teacher) {
    tv_name_of_teacher.text = teacher.nameOfTeacher
    tv_name_of_lesson.text = teacher.nameOfLesson
  }

  fun setAssessment(assessment: String) {
    (tv_assessment_of_teacher.background as GradientDrawable)
        .setColor(Utils.getColorByAssessment(context, assessment))
    tv_assessment_of_teacher.text = assessment
  }

  override fun onClick(v: View?) {
    when (v?.id) {
      R.id.tv_assessment_of_teacher -> {
        showAssessments()
      }
      R.id.tv_container -> {
      }
    }
  }

  fun showAssessments() {
    context.selector("Как вы оцениваете преподавателя?",
        listOf("Отлично", "Хорошо", "Нормально", "Так себе", "Плохо")) { position ->
      when (position) {
        0 -> setAssessment("A")
        1 -> setAssessment("B")
        2 -> setAssessment("C")
        3 -> setAssessment("D")
        4 -> setAssessment("E")
      }
    }
  }
}