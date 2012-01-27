/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.tools.lint.checks;

import com.android.util.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementation of {@link ApiClass}
 *
 */
public class ApiClassImpl implements ApiClass {

    private final String mName;
    private final int mSince;

    private final List<Pair<String, Integer>> mSuperClasses = new ArrayList<Pair<String, Integer>>();
    private final List<Pair<String, Integer>> mInterfaces = new ArrayList<Pair<String, Integer>>();

    private final Map<String, Integer> mFields = new HashMap<String, Integer>();
    private final Map<String, Integer> mMethods = new HashMap<String, Integer>();

    public ApiClassImpl(String name, int since) {
        mName = name;
        mSince = since;
    }

    @Override
    public String getName() {
        return mName;
    }

    @Override
    public int getSince() {
        return mSince;
    }

    @Override
    public Integer getField(String name, Api info) {
        // The field can come from this class or from a super class or an interface
        // The value can never be lower than this introduction of this class.
        // When looking at super classes and interfaces, it can never be lower than when the
        // super class or interface was added as a super class or interface to this clas.
        // Look at all the values and take the lowest.
        // For instance:
        // This class A is introduced in 5 with super class B.
        // In 10, the interface C was added.
        // Looking for SOME_FIELD we get the following:
        // Present in A in API 15
        // Present in B in API 11
        // Present in C in API 7.
        // The answer is 10, which is when C became an interface
        int min = Integer.MAX_VALUE;
        Integer i = mFields.get(name);
        if (i != null) {
            min = i;
        }

        // now look at the super classes
        for (Pair<String, Integer> superClassPair : mSuperClasses) {
            ApiClass superClass = info.getClass(superClassPair.getFirst());
            if (superClass != null) {
                i = superClass.getField(name, info);
                if (i != null) {
                    int tmp = superClassPair.getSecond() > i ? superClassPair.getSecond() : i;
                    if (tmp < min) {
                        min = tmp;
                    }
                }
            }
        }

        // now look at the interfaces
        for (Pair<String, Integer> superClassPair : mInterfaces) {
            ApiClass superClass = info.getClass(superClassPair.getFirst());
            if (superClass != null) {
                i = superClass.getField(name, info);
                if (i != null) {
                    int tmp = superClassPair.getSecond() > i ? superClassPair.getSecond() : i;
                    if (tmp < min) {
                        min = tmp;
                    }
                }
            }
        }

        return min;
    }

    @Override
    public int getMethod(String methodSignature, Api info) {
        // The method can come from this class or from a super class.
        // The value can never be lower than this introduction of this class.
        // When looking at super classes, it can never be lower than when the super class became
        // a super class of this class.
        // Look at all the values and take the lowest.
        // For instance:
        // This class A is introduced in 5 with super class B.
        // In 10, the super class changes to C.
        // Looking for foo() we get the following:
        // Present in A in API 15
        // Present in B in API 11
        // Present in C in API 7.
        // The answer is 10, which is when C became the super class.
        int min = Integer.MAX_VALUE;
        Integer i = mMethods.get(methodSignature);
        if (i != null) {
            min = i;
        }

        // now look at the super classes
        for (Pair<String, Integer> superClassPair : mSuperClasses) {
            ApiClass superClass = info.getClass(superClassPair.getFirst());
            if (superClass != null) {
                i = superClass.getMethod(methodSignature, info);
                if (i != null) {
                    int tmp = superClassPair.getSecond() > i ? superClassPair.getSecond() : i;
                    if (tmp < min) {
                        min = tmp;
                    }
                }
            }
        }

        return min;
    }

    public void addField(String name, int since) {
        Integer i = mFields.get(name);
        if (i == null || i.intValue() > since) {
            mFields.put(name, Integer.valueOf(since));
        }
    }

    public void addMethod(String name, int since) {
        Integer i = mMethods.get(name);
        if (i == null || i.intValue() > since) {
            mMethods.put(name, Integer.valueOf(since));
        }
    }

    public void addSuperClass(String superClass, int since) {
        addToArray(mSuperClasses, superClass, since);
    }

    public void addInterface(String interfaceClass, int since) {
        addToArray(mInterfaces, interfaceClass, since);
    }

    void addToArray(List<Pair<String, Integer>> list, String name, int value) {
        // check if we already have that name (at a lower level)
        for (Pair<String, Integer> pair : list) {
            if (name.equals(pair.getFirst())) {
                return;
            }
        }

        list.add(Pair.of(name, Integer.valueOf(value)));

    }

    @Override
    public String toString() {
        return mName;
    }
}