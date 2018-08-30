/*
 * This file is part of the Androidino
 *
 * Copyright (C) 2017 Mundo Reader S.L.
 *
 * Date: August 2018
 * Author: Estefanía Sarasola Elvira <estefania.sarasola@bq.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.bq.robotic.droid2ino.utils

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException

/**
 * Simple validator for a JSON using Gson.
 */
object GsonValidator {

    private val gson: Gson by lazy { Gson() }

    /**
     * Function that checks that the string given by parameter is a valid Json object by trying to
     * parse it.
     *
     * @param json  A json in String format
     *
     */
    fun isJsonValid(json: String): Boolean {
        return try {
            gson.fromJson(json, Object::class.java)
            true
        } catch (e: JsonSyntaxException) {
            Log.v(GsonValidator.javaClass.simpleName, "Json is not valid, it's malformed")
            false
        }
    }

}