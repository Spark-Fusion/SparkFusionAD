package com.sparkfusionad.sdk.freelybase.bean

import io.freelybase.android.FreelyObject
import io.freelybase.android.FreelyRelation
import io.freelybase.android.Pointer
import io.freelybase.android.Relation

/**
 *作者：daboluo on 2026/7/13 23:38
 *Email:daboluo719@gmail.com
 */
class Adspace: FreelyObject() {
    var name: String? = null//名字
    var type: String?=null//广告类型
    @Pointer
    var game:Game?=null//
    @Relation
    var adcontent: FreelyRelation<Addata> = FreelyRelation()
}