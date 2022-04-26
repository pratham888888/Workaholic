package com.pratham.workaholic.models

import android.os.Parcel
import android.os.Parcelable
import com.google.android.gms.common.internal.safeparcel.SafeParcelWriter
import com.google.android.gms.common.internal.safeparcel.SafeParcelWriter.writeString

data class Card(
    val name: String = "",
    val createdBy: String = "",
    val assignedTo: ArrayList<String> = ArrayList(),
    val labelColor:String="",
    val dueDate:Long=0

) : Parcelable {
    constructor(source: Parcel) : this(
        source.readString()!!,
        source.readString()!!,
        source.createStringArrayList()!!,
        source.readString()!!,
        source.readLong()!!
    )

    override fun describeContents() = 0

    override fun writeToParcel(dest: Parcel, flags: Int) = with(dest) {
        writeString(name)
        writeString(createdBy)
        writeStringList(assignedTo)
        writeString(labelColor)
        writeLong(dueDate)
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<Card> = object : Parcelable.Creator<Card> {
            override fun createFromParcel(source: Parcel): Card = Card(source)
            override fun newArray(size: Int): Array<Card?> = arrayOfNulls(size)
        }
    }
}