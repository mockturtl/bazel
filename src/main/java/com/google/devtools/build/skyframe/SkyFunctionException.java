// Copyright 2014 Google Inc. All rights reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package com.google.devtools.build.skyframe;


import com.google.common.base.Preconditions;

/**
 * Base class of exceptions thrown by {@link SkyFunction#compute} on failure.
 *
 * SkyFunctions should declare a subclass {@code C} of {@link SkyFunctionException} whose
 * constructors forward fine-grained exception types (e.g. {@link IOException}) to
 * {@link SkyFunctionException}'s constructor, and they should also declare
 * {@link SkyFunction#compute} to throw {@code C}. This way the type system checks that no
 * unexpected exceptions are thrown by the {@link SkyFunction}.
 *
 * <p>We took this approach over using a generic exception class since Java disallows it because of
 * type erasure
 * (see http://docs.oracle.com/javase/tutorial/java/generics/restrictions.html#cannotCatch).
 *
 * <p>Failures are explicitly either transient or persistent. The transience of the failure from
 * {@link SkyFunction#compute} should be influenced only by the computations done, and not by the
 * transience of the failures from computations requested via
 * {@link SkyFunction.Environment#getValueOrThrow}.
 */
public abstract class SkyFunctionException extends Exception {

  /** The transience of the error. */
  public enum Transience {
    // An error that may or may not occur again if the computation were re-run. If a computation
    // results in a transient error and is needed on a subsequent MemoizingEvaluator#evaluate call,
    // it will be re-executed.
    TRANSIENT,

    // An error that is completely deterministic and persistent in terms of the computation's
    // inputs. Persistent errors may be cached.
    PERSISTENT;
  }

  private final SkyKey rootCause;
  private final Transience transience;

  public SkyFunctionException(SkyKey rootCause, Throwable cause, Transience transience) {
    super(Preconditions.checkNotNull(cause));
    // TODO(bazel-team): Consider getting rid of custom root causes since they can't be trusted.
    this.rootCause = Preconditions.checkNotNull(rootCause, cause);
    this.transience = transience;
  }

  final SkyKey getRootCauseSkyKey() {
    return rootCause;
  }

  final boolean isTransient() {
    return transience == Transience.TRANSIENT;
  }

  /**
   * Catastrophic failures halt the build even when in keepGoing mode.
   */
  public boolean isCatastrophic() {
    return false;
  }
}
