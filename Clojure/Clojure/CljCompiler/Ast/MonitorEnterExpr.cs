﻿/**
 *   Copyright (c) Rich Hickey. All rights reserved.
 *   The use and distribution terms for this software are covered by the
 *   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
 *   which can be found in the file epl-v10.html at the root of this distribution.
 *   By using this software in any fashion, you are agreeing to be bound by
 * 	 the terms of this license.
 *   You must not remove this notice, or any other, from this software.
 **/

/**
 *   Author: David Miller
 **/

using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
#if CLR2
using Microsoft.Scripting.Ast;
#else
using System.Linq.Expressions;
#endif

namespace clojure.lang.CljCompiler.Ast
{
    class MonitorEnterExpr : UntypedExpr
    {
        #region Data

        readonly Expr _target;

        #endregion

        #region Ctors

        public MonitorEnterExpr(Expr target)
        {
            _target = target;
        }

        #endregion

        #region Parsing

        public sealed class Parser : IParser
        {
            public Expr Parse(object form, bool isRecurContext)
            {
                return new MonitorEnterExpr(Compiler.GenerateAST(RT.second(form),false));
            }
        }

        #endregion

        #region Code generation

        public override Expression GenDlr(GenContext context)
        {
            return Expression.Block(
                Expression.Call(Compiler.Method_Monitor_Enter, _target.GenDlr(context)),
                Compiler.NIL_EXPR.GenDlr(context));
        }

        #endregion

    }
}
