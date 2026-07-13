package com.sparkfusionad.sdk.freelybase.bean

import io.freelybase.android.FreelyFile
import io.freelybase.android.FreelyObject

/**
 *作者：daboluo on 2026/7/13 23:38
 *Email:daboluo719@gmail.com
 */
class Addata: FreelyObject() {
    var appname: String?=null//app名字
    var content:String?=null//内容
    var applogo: FreelyFile?=null//应用logo
    var promotional: FreelyFile?=null//宣传图
    var version:String?=null//版本
    var isDownload: Boolean?=null//是否下载（true为下载，false为直接打开链接）
    var url:String?=null//链接
    var weight: Int=0//（0-100）越大展示频率越高
}