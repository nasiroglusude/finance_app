package com.example.financeapp.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.financeapp.R
import com.example.financeapp.model.DateCalendar
import java.util.ArrayList

class HorizontalCalendarAdapter(private val listener: (calendarDateModel: DateCalendar, position: Int) -> Unit) :
    RecyclerView.Adapter<HorizontalCalendarAdapter.CalendarViewHolder>() {
    private var list = ArrayList<DateCalendar>()

    interface OnItemClickListener {
        fun onItemClick(ddMmYy: String, dd: String, day: String)
    }

    private var mListener: OnItemClickListener? = null

    fun setOnItemClickListener(listener: OnItemClickListener) {
        mListener = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CalendarViewHolder {
        val inflater: LayoutInflater = LayoutInflater.from(parent.context)
        val view: View = inflater.inflate(R.layout.date_layout, parent, false)
        return CalendarViewHolder(view)
    }

    override fun onBindViewHolder(holder: CalendarViewHolder, position: Int) {
        val itemList = list[position]
        holder.bind(itemList, position, listener, mListener)
    }

    override fun getItemCount(): Int {
        return list.size
    }

    class CalendarViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        private val calendarDay: TextView = itemView.findViewById(R.id.tv_calendar_day)
        private val calendarDate: TextView = itemView.findViewById(R.id.tv_calendar_date)
        private val linear: LinearLayout = itemView.findViewById(R.id.linear_calendar)

        fun bind(
            itemList: DateCalendar,
            position: Int,
            listener: (calendarDateModel: DateCalendar, position: Int) -> Unit,
            itemClickListener: OnItemClickListener?
        ) {
            calendarDay.text = itemList.calendarDay
            calendarDate.text = itemList.calendarDate

            itemView.setOnClickListener {
                val adapterPosition = adapterPosition
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    val text = itemList.calendarYear
                    val date = itemList.calendarDate
                    val day = itemList.calendarDay
                    itemClickListener?.onItemClick(text, date, day)
                    listener.invoke(itemList, adapterPosition)
                }
            }

            if (itemList.isSelected) {
                calendarDay.setTextColor(ContextCompat.getColor(itemView.context, R.color.white))
                calendarDate.setTextColor(ContextCompat.getColor(itemView.context, R.color.white))
                linear.setBackgroundResource(R.drawable.rectangle_fill)
            } else {
                calendarDay.setTextColor(ContextCompat.getColor(itemView.context, R.color.black))
                calendarDate.setTextColor(ContextCompat.getColor(itemView.context, R.color.black))
                linear.setBackgroundResource(R.drawable.rectangle_outline)
            }
        }

    }

    fun setData(calendarList: ArrayList<DateCalendar>) {
        list.clear()
        list.addAll(calendarList)
        notifyDataSetChanged()
    }
}
