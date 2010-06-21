;   Copyright (c) Rich Hickey. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns #^{:doc "The core Clojure language."
       :author "Rich Hickey"}
  clojure.core)

(def unquote)
(def unquote-splicing)
 
(def
 #^{:arglists '([& items])
    :doc "Creates a new list containing the items."}
  list (. clojure.lang.PersistentList creator))

(def
 #^{:arglists '([x seq])
    :doc "Returns a new seq where x is the first element and seq is
    the rest."}

 cons (fn* cons [x seq] (. clojure.lang.RT (cons x seq))))

 ;during bootstrap we don't have destructuring let, loop or fn, will redefine later
(def
 #^{:macro true}
  let (fn* let [&form &env & decl] (cons 'let* decl)))
  
(def
 #^{:macro true}
 loop (fn* loop [&form &env & decl] (cons 'loop* decl)))  
 
 (def
 #^{:macro true}
 fn (fn* fn [&form &env & decl] 
         (.withMeta #^clojure.lang.IObj (cons 'fn* decl) 
                    (.meta #^clojure.lang.IMeta &form))))
  
(def 
 #^{:arglists '([coll])
    :doc "Returns the first item in the collection. Calls seq on its
    argument. If coll is nil, returns nil."}
 first (fn first [coll] (. clojure.lang.RT (first coll))))

(def   
 #^{:arglists '([coll])
    :tag clojure.lang.ISeq
    :doc "Returns a seq of the items after the first. Calls seq on its
  argument.  If there are no more items, returns nil."}  
 next (fn next [x] (. clojure.lang.RT (next x))))

(def
 #^{:arglists '([coll])
    :tag clojure.lang.ISeq
    :doc "Returns a possibly empty seq of the items after the first. Calls seq on its
  argument."}  
 rest (fn rest [x] (. clojure.lang.RT (more x))))
 
(def
 #^{:arglists '([coll x] [coll x & xs])
    :doc "conj[oin]. Returns a new collection with the xs
    'added'. (conj nil item) returns (item).  The 'addition' may
    happen at different 'places' depending on the concrete type."}
 conj (fn conj 
        ([coll x] (. clojure.lang.RT (conj coll x)))
        ([coll x & xs]
         (if xs
           (recur (conj coll x) (first xs) (next xs))
           (conj coll x)))))
           
(def
 #^{:doc "Same as (first (next x))"
    :arglists '([x])}
 second (fn second [x] (first (next x))))

(def
 #^{:doc "Same as (first (first x))"
    :arglists '([x])}
 ffirst (fn ffirst [x] (first (first x))))

(def
 #^{:doc "Same as (next(first x))"
    :arglists '([x])}
 nfirst (fn nfirst [x] (next (first x))))

(def
 #^{:doc "Same as (first (next x))"
    :arglists '([x])}
 fnext (fn fnext [x] (first (next x))))

(def
 #^{:doc "Same as (next (next x))"
    :arglists '([x])}
 nnext (fn rrest [x] (next (next x))))

(def
 #^{:arglists '([coll])
    :doc "Returns a seq on the collection. If the collection is 
    empty, returns nil.  (seq nil) returns nil. seq also works on 
    Strings, native Java arrays (of reference types) and any objects 
    that implement Iterable."
    :tag clojure.lang.ISeq}
 seq (fn seq [coll] (. clojure.lang.RT (seq coll))))

(def   ;;;  Had do change Class to Type and isInstance to IsInstanceOfType
 #^{:arglists '([#^Class c x])
    :doc "Evaluates x and tests if it is an instance of the class
    c. Returns true or false"}
 instance? (fn instance? [#^Type c x] (. c (IsInstanceOfType x))))
 
(def
 #^{:arglists '([x])
    :doc "Return true if x implements ISeq"}
 seq? (fn seq? [x] (instance? clojure.lang.ISeq x)))

(def
  #^{:arglists '([x])
    :doc "Return true if x is a Character"}
 char? (fn char? [x] (instance? Char x)))             ;;; Character

(def
 #^{:arglists '([x])
    :doc "Return true if x is a String"}
 string? (fn string? [x] (instance? String x)))

(def
 #^{:arglists '([x])
    :doc "Return true if x implements IPersistentMap"}
 map? (fn map? [x] (instance? clojure.lang.IPersistentMap x)))

(def
 #^{:arglists '([x])
    :doc "Return true if x implements IPersistentVector "}
 vector? (fn vector? [x] (instance? clojure.lang.IPersistentVector x)))

(def
 #^{:arglists '([map key val] [map key val & kvs])
    :doc "assoc[iate]. When applied to a map, returns a new map of the
    same (hashed/sorted) type, that contains the mapping of key(s) to
    val(s). When applied to a vector, returns a new vector that
    contains val at index. Note - index must be <= (count vector)."}
 assoc
 (fn assoc
   ([map key val] (. clojure.lang.RT (assoc map key val)))
   ([map key val & kvs]
    (let [ret (assoc map key val)]
      (if kvs
        (recur ret (first kvs) (second kvs) (nnext kvs))
        ret)))))
        
;;;;;;;;;;;;;;;;; metadata ;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def
 #^{:arglists '([obj])
    :doc "Returns the metadata of obj, returns nil if there is no metadata."}
 meta (fn meta [x]
        (if (instance? clojure.lang.IMeta x)
          (. #^clojure.lang.IMeta x (meta)))))

(def
 #^{:arglists '([#^clojure.lang.IObj obj m])
    :doc "Returns an object of the same type and value as obj, with
    map m as its metadata."}
 with-meta (fn with-meta [#^clojure.lang.IObj x m]
             (. x (withMeta m))))

(def
 #^{:private true}
 sigs
 (fn [fdecl]
   (let [asig 
         (fn [fdecl]
           (let [arglist (first fdecl)
                 ;elide implicit macro args
                 arglist (if (clojure.lang.Util/equals '&form (first arglist)) 
                           (clojure.lang.RT/subvec arglist 2 (clojure.lang.RT/count arglist))
                           arglist)           
                 body (next fdecl)]
             (if (map? (first body))
               (if (next body)
                 (with-meta arglist (conj (if (meta arglist) (meta arglist) {}) (first body)))
                 arglist)
               arglist)))]
     (if (seq? (first fdecl))
       (loop [ret [] fdecls fdecl]
         (if fdecls
           (recur (conj ret (asig (first fdecls))) (next fdecls))
           (seq ret)))
       (list (asig fdecl))))))


(def 
 #^{:arglists '([coll])
    :doc "Return the last item in coll, in linear time"}
 last (fn last [s]
        (if (next s)
          (recur (next s))
          (first s))))
      
(def 
 #^{:arglists '([coll])
    :doc "Return a seq of all but the last item in coll, in linear time"}
 butlast (fn butlast [s]
           (loop [ret [] s s]
             (if (next s)
               (recur (conj ret (first s)) (next s))
               (seq ret)))))  

(def 

 #^{:doc "Same as (def name (fn [params* ] exprs*)) or (def
    name (fn ([params* ] exprs*)+)) with any doc-string or attrs added
    to the var metadata"
    :arglists '([name doc-string? attr-map? [params*] body]
                [name doc-string? attr-map? ([params*] body)+ attr-map?])}
 defn (fn defn [&form &env name & fdecl]
        (let [m (if (string? (first fdecl))
                  {:doc (first fdecl)}
                  {})
              fdecl (if (string? (first fdecl))
                      (next fdecl)
                      fdecl)
              m (if (map? (first fdecl))
                  (conj m (first fdecl))
                  m)
              fdecl (if (map? (first fdecl))
                      (next fdecl)
                      fdecl)
              fdecl (if (vector? (first fdecl))
                      (list fdecl)
                      fdecl)
              m (if (map? (last fdecl))
                  (conj m (last fdecl))
                  m)
              fdecl (if (map? (last fdecl))
                      (butlast fdecl)
                      fdecl)
              m (conj {:arglists (list 'quote (sigs fdecl))} m)
              m (let [inline (:inline m)
                      ifn (first inline)
                      iname (second inline)]
                  ;; same as: (if (and (= 'fn ifn) (not (symbol? iname))) ...)
                  (if (if (clojure.lang.Util/equiv 'fn ifn)
                        (if (instance? clojure.lang.Symbol iname) false true))
                    ;; inserts the same fn name to the inline fn if it does not have one
                    (assoc m :inline (cons ifn (cons name (next inline))))
                    m))
              m (conj (if (meta name) (meta name) {}) m)]
          (list 'def (with-meta name m)
               (list '.withMeta (cons `fn (cons name fdecl)) (list '.meta (list 'var name)))))))

(. (var defn) (setMacro))       
;;; Not the same as the Java version, but good enough?
(defn cast
  "Throws a ClassCastException if x is not a c, else returns x."
  [#^Type c x]   ;;; changed Class to Type
   (if (. c (IsInstanceOfType x)) x (throw  (InvalidCastException. "Unable to cast."))))  ;;;  original (. c (cast x)))     

(defn to-array
  "Returns an array of Objects containing the contents of coll, which
  can be any Collection.  Maps to java.util.Collection.toArray()."
  {:tag "System.Object[]" }                                                  ;;;{:tag "[Ljava.lang.Object;"}  
  [coll] (. clojure.lang.RT (toArray coll)))
 
(defn vector
  "Creates a new vector containing the args."
  ([] [])
  ([& args]
   (. clojure.lang.LazilyPersistentVector (create args))))

(defn vec
  "Creates a new vector containing the contents of coll."
  ([coll]
   (if (instance? System.Collections.ICollection coll)                                        ;;;   java.util.Collection
     (clojure.lang.LazilyPersistentVector/create coll)                                       
     (. clojure.lang.LazilyPersistentVector (createOwning (to-array coll))))))

(defn hash-map
  "keyval => key val
  Returns a new hash map with supplied mappings."
  ([] {})
  ([& keyvals]
   (. clojure.lang.PersistentHashMap (createWithCheck keyvals))))

(defn hash-set
  "Returns a new hash set with supplied keys."
  ([] #{})
  ([& keys]
   (clojure.lang.PersistentHashSet/createWithCheck keys)))

(defn sorted-map
  "keyval => key val
  Returns a new sorted map with supplied mappings."
  ([] clojure.lang.PersistentTreeMap/EMPTY)                    ;;; I HAD TO ADD THIS EXTRA CASE TO AVOID AMBIGUOUS CALL TO CREATE WITH NULL
  ([& keyvals]    (. clojure.lang.PersistentTreeMap (create keyvals))))

(defn sorted-map-by
  "keyval => key val
  Returns a new sorted map with supplied mappings, using the supplied comparator."
  ([comparator & keyvals]
   (clojure.lang.PersistentTreeMap/create comparator keyvals)))
 
(defn sorted-set  
  "Returns a new sorted set with supplied keys."
  ([] clojure.lang.PersistentTreeSet/EMPTY)                    ;;; I HAD TO ADD THIS EXTRA CASE TO AVOID AMBIGUOUS CALL TO CREATE WITH NULL
  ([& keys]   (clojure.lang.PersistentTreeSet/create keys))) 
 
(defn sorted-set-by
  "Returns a new sorted set with supplied keys, using the supplied comparator."
  ([comparator & keys]
   (clojure.lang.PersistentTreeSet/create comparator keys))) 
 
 
;;;;;;;;;;;;;;;;;;;;
(defn nil?
  "Returns true if x is nil, false otherwise."
  {:tag Boolean}
  [x] (clojure.lang.Util/identical x nil))

(def

 #^{:doc "Like defn, but the resulting function name is declared as a
  macro and will be used as a macro by the compiler when it is
  called."
    :arglists '([name doc-string? attr-map? [params*] body]
                [name doc-string? attr-map? ([params*] body)+ attr-map?])}
  defmacro (fn [&form &env 
                name & args]
             (let [prefix (loop [p (list name) args args]
                            (let [f (first args)]
                              (if (string? f)
                                (recur (cons f p) (next args))
                                (if (map? f)
                                  (recur (cons f p) (next args))
                                  p))))
                   fdecl (loop [fd args]
                           (if (string? (first fd))
                             (recur (next fd))
                             (if (map? (first fd))
                               (recur (next fd))
                               fd)))
                   fdecl (if (vector? (first fdecl))
                           (list fdecl)
                           fdecl)
                   add-implicit-args (fn [fd]
                             (let [args (first fd)]
                               (cons (vec (cons '&form (cons '&env args))) (next fd))))
                   add-args (fn [acc ds]
                              (if (nil? ds)
                                acc
                                (let [d (first ds)]
                                  (if (map? d)
                                    (conj acc d)
                                    (recur (conj acc (add-implicit-args d)) (next ds))))))
                   fdecl (seq (add-args [] fdecl))
                   decl (loop [p prefix d fdecl]
                          (if p
                            (recur (next p) (cons (first p) d))
                            d))]
               (list 'do
                     (cons `defn decl)
                     (list '. (list 'var name) '(setMacro))
                     (list 'var name)))))


(. (var defmacro) (setMacro))

(defmacro when
  "Evaluates test. If logical true, evaluates body in an implicit do."
  [test & body]
  (list 'if test (cons 'do body)))

(defmacro when-not
  "Evaluates test. If logical false, evaluates body in an implicit do."
  [test & body]
    (list 'if test nil (cons 'do body)))

(defn false?
  "Returns true if x is the value false, false otherwise."
  {:tag Boolean}
  [x] (clojure.lang.Util/identical x false))

(defn true?
  "Returns true if x is the value true, false otherwise."
  {:tag Boolean}
  [x] (clojure.lang.Util/identical x true))

(defn not
  "Returns true if x is logical false, false otherwise."
  {:tag Boolean}
  [x] (if x false true))

(defn str
  "With no args, returns the empty string. With one arg x, returns
  x.toString().  (str nil) returns the empty string. With more than
  one arg, returns the concatenation of the str values of the args."
  {:tag String}
  ([] "")
  ([#^Object x]
   (if (nil? x) "" (String/Format System.Globalization.CultureInfo/InvariantCulture "{0}" x)))    ;;;(if (nil? x) "" (. x (toString))))   ;; java: toString
  ([x & ys]
     ((fn [#^StringBuilder sb more]
          (if more
            (recur (. sb  (Append (str (first more)))) (next more))  ;; java: append
            (str sb)))
      (new StringBuilder #^String (str x)) ys)))


(defn symbol?
  "Return true if x is a Symbol"
  [x] (instance? clojure.lang.Symbol x))

(defn keyword?
  "Return true if x is a Keyword"
  [x] (instance? clojure.lang.Keyword x))

(defn symbol
  "Returns a Symbol with the given namespace and name."
  {:tag clojure.lang.Symbol}
  ([name] (if (symbol? name) name (clojure.lang.Symbol/intern name)))
  ([ns name] (clojure.lang.Symbol/intern ns name)))

(defn gensym
  "Returns a new symbol with a unique name. If a prefix string is
  supplied, the name is prefix# where # is some unique number. If
  prefix is not supplied, the prefix is 'G__'."
  ([] (gensym "G__"))
  ([prefix-string] (. clojure.lang.Symbol (intern (str prefix-string (str (. clojure.lang.RT (nextID))))))))

(defmacro cond
  "Takes a set of test/expr pairs. It evaluates each test one at a
  time.  If a test returns logical true, cond evaluates and returns
  the value of the corresponding expr and doesn't evaluate any of the
  other tests or exprs. (cond) returns nil."
  [& clauses]
    (when clauses
      (list 'if (first clauses)
            (if (next clauses)
                (second clauses)
                (throw (ArgumentException.                           ;;;IllegalArgumentException.
                         "cond requires an even number of forms")))
            (cons 'clojure.core/cond (next (next clauses))))))           

(defn keyword
  "Returns a Keyword with the given namespace and name.  Do not use :
  in the keyword strings, it will be added automatically."
  {:tag clojure.lang.Keyword}
  ([name] (cond (keyword? name) name
                (symbol? name) (clojure.lang.Keyword/intern #^clojure.lang.Symbol name)
                (string? name) (clojure.lang.Keyword/intern #^String name)))
  ([ns name] (clojure.lang.Keyword/intern ns name)))

(defn spread
  {:private true}
  [arglist]
  (cond
   (nil? arglist) nil
   (nil? (next arglist)) (seq (first arglist))
   :else (cons (first arglist) (spread (next arglist)))))

(defn list*
  "Creates a new list containing the items prepended to the rest, the
  last of which will be treated as a sequence."
  ([args] (seq args))
  ([a args] (cons a args))
  ([a b args] (cons a (cons b args)))
  ([a b c args] (cons a (cons b (cons c args))))
  ([a b c d & more]
     (cons a (cons b (cons c (cons d (spread more)))))))

(defn apply
  "Applies fn f to the argument list formed by prepending args to argseq."
  { :arglists '([f args* argseq])}    
  ([#^clojure.lang.IFn f args]
     (. f (applyTo (seq args))))
  ([#^clojure.lang.IFn f x args]
     (. f (applyTo (list* x args))))
  ([#^clojure.lang.IFn f x y args]
     (. f (applyTo (list* x y args))))
  ([#^clojure.lang.IFn f x y z args]
     (. f (applyTo (list* x y z args))))
  ([#^clojure.lang.IFn f a b c d & args]
     (. f (applyTo (cons a (cons b (cons c (cons d (spread args)))))))))
    
(defn vary-meta
 "Returns an object of the same type and value as obj, with
  (apply f (meta obj) args) as its metadata."
 [obj f & args]
  (with-meta obj (apply f (meta obj) args)))



(defmacro lazy-seq
  "Takes a body of expressions that returns an ISeq or nil, and yields
  a Seqable object that will invoke the body only the first time seq
  is called, and will cache the result and return it on all subsequent
  seq calls."  
  [& body]
  (list 'new 'clojure.lang.LazySeq (list* '#^{:once true} fn* [] body)))    

(defn #^clojure.lang.ChunkBuffer chunk-buffer [capacity]
  (clojure.lang.ChunkBuffer. capacity))

(defn chunk-append [#^clojure.lang.ChunkBuffer b x]
  (.add b x))

(defn chunk [#^clojure.lang.ChunkBuffer b]
  (.chunk b))

(defn #^clojure.lang.IChunk chunk-first [#^clojure.lang.IChunkedSeq s]
  (.chunkedFirst s))

(defn #^clojure.lang.ISeq chunk-rest [#^clojure.lang.IChunkedSeq s]
  (.chunkedMore s))

(defn #^clojure.lang.ISeq chunk-next [#^clojure.lang.IChunkedSeq s]
  (.chunkedNext s))

(defn chunk-cons [chunk rest]
  (if (clojure.lang.Numbers/isZero (clojure.lang.RT/count chunk))
    rest
    (clojure.lang.ChunkedCons. chunk rest)))
  
(defn chunked-seq? [s]
  (instance? clojure.lang.IChunkedSeq s))

(defn concat
  "Returns a lazy seq representing the concatenation of the elements in the supplied colls."
  ([] (lazy-seq nil))
  ([x] (lazy-seq x))
  ([x y]
    (lazy-seq
      (let [s (seq x)]
        (if s
          (if (chunked-seq? s)
            (chunk-cons (chunk-first s) (concat (chunk-rest s) y))
            (cons (first s) (concat (rest s) y)))
          y))))
  ([x y & zs]
     (let [cat (fn cat [xys zs]
                 (lazy-seq
                   (let [xys (seq xys)]
                     (if xys
                       (if (chunked-seq? xys)
                         (chunk-cons (chunk-first xys)
                                     (cat (chunk-rest xys) zs))
                         (cons (first xys) (cat (rest xys) zs)))
                       (when zs
                         (cat (first zs) (next zs)))))))]
       (cat (concat x y) zs))))

;;;;;;;;;;;;;;;;at this point all the support for syntax-quote exists;;;;;;;;;;;;;;;;;;;;;;


(defmacro delay
  "Takes a body of expressions and yields a Delay object than will
  invoke the body only the first time it is forced (with force or deref/@), and
  will cache the result and return it on all subsequent force
  calls."
  [& body]
    (list 'new 'clojure.lang.Delay (list* `#^{:once true} fn* [] body)))

(defn delay?
  "returns true if x is a Delay created with delay"
  [x] (instance? clojure.lang.Delay x))

(defn force
  "If x is a Delay, returns the (possibly cached) value of its expression, else returns x"
  [x] (. clojure.lang.Delay (force x)))
    
(defmacro if-not
  "Evaluates test. If logical false, evaluates and returns then expr, 
  otherwise else expr, if supplied, else nil."  
  ([test then] `(if-not ~test ~then nil))
  ([test then else]
   `(if (not ~test) ~then ~else)))
   
(defn identical?
  "Tests if 2 arguments are the same object"
  {:tag Boolean
   :inline (fn [x y] `(. clojure.lang.Util identical ~x ~y))
   :inline-arities #{2}}
  ([x y] (clojure.lang.Util/identical x y)))

(defn =
  "Equality. Returns true if x equals y, false if not. Same as
  Java x.equals(y) except it also works for nil, and compares
  numbers and collections in a type-independent manner.  Clojure's immutable data
  structures define equals() (and thus =) as a value, not an identity,
  comparison."
  {:tag Boolean
   :inline (fn [x y] `(. clojure.lang.Util equiv ~x ~y))  
   :inline-arities #{2}}
  ([x] true)
  ([x y] (clojure.lang.Util/equiv x y))
  ([x y & more]
   (if (= x y)
     (if (next more)
       (recur y (first more) (next more))
       (= y (first more)))
     false)))

(defn not=
  "Same as (not (= obj1 obj2))"
  {:tag Boolean}
  ([x] false)
  ([x y] (not (= x y)))
  ([x y & more]
   (not (apply = x y more))))  



(defn compare
  "Comparator. Returns a negative number, zero, or a positive number
  when x is logically 'less than', 'equal to', or 'greater than'
  y. Same as Java x.compareTo(y) except it also works for nil, and
  compares numbers and collections in a type-independent manner. x
  must implement Comparable"
  {:tag Int32  ;;; was Integer
   :inline (fn [x y] `(. clojure.lang.Util compare ~x ~y))}
  [x y] (. clojure.lang.Util (compare x y)))

(defmacro and
  "Evaluates exprs one at a time, from left to right. If a form
  returns logical false (nil or false), and returns that value and
  doesn't evaluate any of the other expressions, otherwise it returns
  the value of the last expr. (and) returns true."
  ([] true)
  ([x] x)
  ([x & next]
   `(let [and# ~x]
      (if and# (and ~@next) and#))))

(defmacro or
  "Evaluates exprs one at a time, from left to right. If a form
  returns a logical true value, or returns that value and doesn't
  evaluate any of the other expressions, otherwise it returns the
  value of the last expression. (or) returns nil."
  ([] nil)
  ([x] x)
  ([x & next]
      `(let [or# ~x]
         (if or# or# (or ~@next)))))

;;;;;;;;;;;;;;;;;;; sequence fns  ;;;;;;;;;;;;;;;;;;;;;;;
(defn zero?
  "Returns true if num is zero, else false"
  {:tag Boolean
   :inline (fn [x] `(. clojure.lang.Numbers (isZero ~x)))}
  [x] (. clojure.lang.Numbers (isZero x)))
  
(defn count
  "Returns the number of items in the collection. (count nil) returns
  0.  Also works on strings, arrays, and Java Collections and Maps"
  {:tag Int32                                                               ;;; Integer
   :inline (fn  [x] `(. clojure.lang.RT (count ~x)))}
  [coll] (. clojure.lang.RT (count coll)))

(defn int                                    ;;; Need to make this handle args out of range
  "Coerce to int"
  {:tag Int32   ;;; Integer
   :inline (fn  [x] `(. clojure.lang.RT (intCast ~x)))}
  [x] (. clojure.lang.RT (intCast x)))

(defn nth
  "Returns the value at the index. get returns nil if index out of
  bounds, nth throws an exception unless not-found is supplied.  nth
  also works for strings, Java arrays, regex Matchers and Lists, and,
  in O(n) time, for sequences."
  {:inline (fn  [c i & nf] `(. clojure.lang.RT (nth ~c ~i ~@nf)))
   :inline-arities #{2 3}}  
  ([coll index] (. clojure.lang.RT (nth coll index)))
  ([coll index not-found] (. clojure.lang.RT (nth coll index not-found))))
  
(defn <
  "Returns non-nil if nums are in monotonically increasing order,
  otherwise false."
  {:inline (fn [x y] `(. clojure.lang.Numbers (lt ~x ~y)))
   :inline-arities #{2}}
  ([x] true)
  ([x y] (. clojure.lang.Numbers (lt x y)))
  ([x y & more]
   (if (< x y)
     (if (next more)
       (recur y (first more) (next more))
       (< y (first more)))
     false)))
     
(defn inc
  "Returns a number one greater than num."
  {:inline (fn [x] `(. clojure.lang.Numbers (inc ~x)))}
  [x] (. clojure.lang.Numbers (inc x)))

;; reduce is defined again later after InternalReduce loads
(defn reduce
  ([f coll]
   (let [s (seq coll)]
     (if s
       (reduce f (first s) (next s))
       (f))))
  ([f val coll]
     (let [s (seq coll)]
       (if s
         (if (chunked-seq? s)
           (recur f 
                  (.reduce (chunk-first s) f val)
                  (chunk-next s))
           (recur f (f val (first s)) (next s)))
          val))))   
              
(defn reverse
  "Returns a seq of the items in coll in reverse order. Not lazy."
  [coll]
    (reduce conj () coll))

;;math stuff
(defn +
  "Returns the sum of nums. (+) returns 0."
  {:inline (fn [x y] `(. clojure.lang.Numbers (add ~x ~y)))
   :inline-arities #{2}}
  ([] 0)
  ([x] (. clojure.lang.RT (NumberCast x)))         ;; (cast Number x))
  ([x y] (. clojure.lang.Numbers (add x y)))
  ([x y & more]
   (reduce + (+ x y) more)))

(defn *
  "Returns the product of nums. (*) returns 1."
  {:inline (fn [x y] `(. clojure.lang.Numbers (multiply ~x ~y)))
   :inline-arities #{2}}
  ([] 1)
  ([x] (. clojure.lang.RT (NumberCast x)))         ;; (cast Number x))
  ([x y] (. clojure.lang.Numbers (multiply x y)))
  ([x y & more]
   (reduce * (* x y) more)))

(defn /
  "If no denominators are supplied, returns 1/numerator,
  else returns numerator divided by all of the denominators."
  {:inline (fn [x y] `(. clojure.lang.Numbers (divide ~x ~y)))
   :inline-arities #{2}}
  ([x] (/ 1 x))
  ([x y] (. clojure.lang.Numbers (divide x y)))
  ([x y & more]
   (reduce / (/ x y) more)))

(defn -
  "If no ys are supplied, returns the negation of x, else subtracts
  the ys from x and returns the result."
  {:inline (fn [& args] `(. clojure.lang.Numbers (minus ~@args)))
   :inline-arities #{1 2}}
  ([x] (. clojure.lang.Numbers (minus x)))
  ([x y] (. clojure.lang.Numbers (minus x y)))
  ([x y & more]
   (reduce - (- x y) more)))

(defn <=
  "Returns non-nil if nums are in monotonically non-decreasing order,
  otherwise false."
  {:inline (fn [x y] `(. clojure.lang.Numbers (lte ~x ~y)))
   :inline-arities #{2}}
  ([x] true)
  ([x y] (. clojure.lang.Numbers (lte x y)))
  ([x y & more]
   (if (<= x y)
     (if (next more)
       (recur y (first more) (next more))
       (<= y (first more)))
     false)))

(defn >
  "Returns non-nil if nums are in monotonically decreasing order,
  otherwise false."
  {:inline (fn [x y] `(. clojure.lang.Numbers (gt ~x ~y)))
   :inline-arities #{2}}
  ([x] true)
  ([x y] (. clojure.lang.Numbers (gt x y)))
  ([x y & more]
   (if (> x y)
     (if (next more)
       (recur y (first more) (next more))
       (> y (first more)))
     false)))

(defn >=
  "Returns non-nil if nums are in monotonically non-increasing order,
  otherwise false."
  {:inline (fn [x y] `(. clojure.lang.Numbers (gte ~x ~y)))
   :inline-arities #{2}}
  ([x] true)
  ([x y] (. clojure.lang.Numbers (gte x y)))
  ([x y & more]
   (if (>= x y)
     (if (next more)
       (recur y (first more) (next more))
       (>= y (first more)))
     false)))

(defn ==
  "Returns non-nil if nums all have the same value, otherwise false"
  {:inline (fn [x y] `(. clojure.lang.Numbers (equiv ~x ~y)))
   :inline-arities #{2}}
  ([x] true)
  ([x y] (. clojure.lang.Numbers (equiv x y)))
  ([x y & more]
   (if (== x y)
     (if (next more)
       (recur y (first more) (next more))
       (== y (first more)))
     false)))

(defn max
  "Returns the greatest of the nums."
  ([x] x)
  ([x y] (if (> x y) x y))
  ([x y & more]
   (reduce max (max x y) more)))

(defn min
  "Returns the least of the nums."
  ([x] x)
  ([x y] (if (< x y) x y))
  ([x y & more]
   (reduce min (min x y) more)))

(defn dec
  "Returns a number one less than num."
  {:inline (fn [x] `(. clojure.lang.Numbers (dec ~x)))}
  [x] (. clojure.lang.Numbers (dec x)))  
  
(defn unchecked-inc
  "Returns a number one greater than x, an int or long.
  Note - uses a primitive operator subject to overflow."
  {:inline (fn [x] `(. clojure.lang.Numbers (unchecked_inc ~x)))}
  [x] (. clojure.lang.Numbers (unchecked_inc x)))

(defn unchecked-dec
  "Returns a number one less than x, an int or long.
  Note - uses a primitive operator subject to overflow."
  {:inline (fn [x] `(. clojure.lang.Numbers (unchecked_dec ~x)))}
  [x] (. clojure.lang.Numbers (unchecked_dec x)))

(defn unchecked-negate
  "Returns the negation of x, an int or long.
  Note - uses a primitive operator subject to overflow."
  {:inline (fn [x] `(. clojure.lang.Numbers (unchecked_negate ~x)))}
  [x] (. clojure.lang.Numbers (unchecked_negate x)))

(defn unchecked-add
  "Returns the sum of x and y, both int or long.
  Note - uses a primitive operator subject to overflow."
  {:inline (fn [x y] `(. clojure.lang.Numbers (unchecked_add ~x ~y)))}
  [x y] (. clojure.lang.Numbers (unchecked_add x y)))

(defn unchecked-subtract
  "Returns the difference of x and y, both int or long.
  Note - uses a primitive operator subject to overflow."
  {:inline (fn [x y] `(. clojure.lang.Numbers (unchecked_subtract ~x ~y)))}
  [x y] (. clojure.lang.Numbers (unchecked_subtract x y)))

(defn unchecked-multiply
  "Returns the product of x and y, both int or long.
  Note - uses a primitive operator subject to overflow."
  {:inline (fn [x y] `(. clojure.lang.Numbers (unchecked_multiply ~x ~y)))}
  [x y] (. clojure.lang.Numbers (unchecked_multiply x y)))

(defn unchecked-divide
  "Returns the division of x by y, both int or long.
  Note - uses a primitive operator subject to truncation."
  {:inline (fn [x y] `(. clojure.lang.Numbers (unchecked_divide ~x ~y)))}
  [x y] (. clojure.lang.Numbers (unchecked_divide x y)))

(defn unchecked-remainder
  "Returns the remainder of division of x by y, both int or long.
  Note - uses a primitive operator subject to truncation."
  {:inline (fn [x y] `(. clojure.lang.Numbers (unchecked_remainder ~x ~y)))}
  [x y] (. clojure.lang.Numbers (unchecked_remainder x y)))

(defn pos?
  "Returns true if num is greater than zero, else false"
  {:tag Boolean
   :inline (fn [x] `(. clojure.lang.Numbers (isPos ~x)))}
  [x] (. clojure.lang.Numbers (isPos x)))

(defn neg?
  "Returns true if num is less than zero, else false"
  {:tag Boolean
   :inline (fn [x] `(. clojure.lang.Numbers (isNeg ~x)))}
  [x] (. clojure.lang.Numbers (isNeg x)))

(defn quot
  "quot[ient] of dividing numerator by denominator."
  [num div]
    (. clojure.lang.Numbers (quotient num div)))

(defn rem
  "remainder of dividing numerator by denominator."
  [num div]
    (. clojure.lang.Numbers (remainder num div)))

(defn rationalize
  "returns the rational value of num"
  [num]
  (. clojure.lang.Numbers (rationalize num)))

;;Bit ops

(defn bit-not
  "Bitwise complement"
  {:inline (fn [x] `(. clojure.lang.Numbers (not ~x)))}
  [x] (. clojure.lang.Numbers not x))


(defn bit-and
  "Bitwise and"
   {:inline (fn [x y] `(. clojure.lang.Numbers (and ~x ~y)))}
  [x y] (. clojure.lang.Numbers and x y))

(defn bit-or
  "Bitwise or"
  {:inline (fn [x y] `(. clojure.lang.Numbers (or ~x ~y)))}
  [x y] (. clojure.lang.Numbers or x y))

(defn bit-xor
  "Bitwise exclusive or"
  {:inline (fn [x y] `(. clojure.lang.Numbers (xor ~x ~y)))}
  [x y] (. clojure.lang.Numbers xor x y))

(defn bit-and-not
  "Bitwise and with complement"
  [x y] (. clojure.lang.Numbers andNot x y))


(defn bit-clear
  "Clear bit at index n"
  [x n] (. clojure.lang.Numbers clearBit x n))

(defn bit-set
  "Set bit at index n"
  [x n] (. clojure.lang.Numbers setBit x n))

(defn bit-flip
  "Flip bit at index n"
  [x n] (. clojure.lang.Numbers flipBit x n))

(defn bit-test
  "Test bit at index n"
  [x n] (. clojure.lang.Numbers testBit x n))


(defn bit-shift-left
  "Bitwise shift left"
  {:inline (fn [x n] `(. clojure.lang.Numbers (shiftLeft ~x ~n)))}
  [x n] (. clojure.lang.Numbers shiftLeft x n))

(defn bit-shift-right
  "Bitwise shift right"
  {:inline (fn [x n] `(. clojure.lang.Numbers (shiftRight ~x ~n)))} 
  [x n] (. clojure.lang.Numbers shiftRight x n))

(defn even?
  "Returns true if n is even, throws an exception if n is not an integer"
  [n] (zero? (bit-and n 1)))

(defn odd?
  "Returns true if n is odd, throws an exception if n is not an integer"
  [n] (not (even? n)))


;;

(defn complement
  "Takes a fn f and returns a fn that takes the same arguments as f,
  has the same effects, if any, and returns the opposite truth value."
  [f] 
  (fn 
    ([] (not (f)))
    ([x] (not (f x)))
    ([x y] (not (f x y)))
    ([x y & zs] (not (apply f x y zs)))))

(defn constantly
  "Returns a function that takes any number of arguments and returns x."
  [x] (fn [& args] x))

(defn identity
  "Returns its argument."
  [x] x)

;;Collection stuff




;;list stuff
(defn peek
  "For a list or queue, same as first, for a vector, same as, but much
  more efficient than, last. If the collection is empty, returns nil."
  [coll] (. clojure.lang.RT (peek coll)))

(defn pop
  "For a list or queue, returns a new list/queue without the first
  item, for a vector, returns a new vector without the last item. If
  the collection is empty, throws an exception.  Note - not the same
  as next/butlast."
  [coll] (. clojure.lang.RT (pop coll)))


;;map stuff

(defn contains?
  "Returns true if key is present in the given collection, otherwise
  returns false.  Note that for numerically indexed collections like
  vectors and Java arrays, this tests if the numeric key is within the
  range of indexes. 'contains?' operates constant or logarithmic time;
  it will not perform a linear search for a value.  See also 'some'."
  [coll key] (. clojure.lang.RT (contains coll key)))

(defn get
  "Returns the value mapped to key, not-found or nil if key not present."
  {:inline (fn  [m k & nf] `(. clojure.lang.RT (get ~m ~k ~@nf)))
   :inline-arities #{2 3}}
  ([map key]
   (. clojure.lang.RT (get map key)))
  ([map key not-found]
   (. clojure.lang.RT (get map key not-found))))

(defn dissoc
  "dissoc[iate]. Returns a new map of the same (hashed/sorted) type,
  that does not contain a mapping for key(s)."
  ([map] map)
  ([map key]
   (. clojure.lang.RT (dissoc map key)))
  ([map key & ks]
   (let [ret (dissoc map key)]
     (if ks
       (recur ret (first ks) (next ks))
       ret))))

(defn disj
  "disj[oin]. Returns a new set of the same (hashed/sorted) type, that
  does not contain key(s)."
  ([set] set)
  ([#^clojure.lang.IPersistentSet set key]
   (. set (disjoin key)))
  ([set key & ks]
   (let [ret (disj set key)]
     (if ks
       (recur ret (first ks) (next ks))
       ret))))

(defn find
  "Returns the map entry for key, or nil if key not present."
  [map key] (. clojure.lang.RT (find map key)))

(defn select-keys
  "Returns a map containing only those entries in map whose key is in keys"
  [map keyseq]
    (loop [ret {} keys (seq keyseq)]
      (if keys
        (let [entry (. clojure.lang.RT (find map (first keys)))]
          (recur
           (if entry
             (conj ret entry)
             ret)
           (next keys)))
        ret)))

(defn keys
  "Returns a sequence of the map's keys."
  [map] (. clojure.lang.RT (keys map)))

(defn vals
  "Returns a sequence of the map's values."
  [map] (. clojure.lang.RT (vals map)))

(defn key
  "Returns the key of the map entry."
  [#^clojure.lang.IMapEntry e]  ;;  [#^java.util.Map$Entry e]
    (. e (key)))                ;; (. e (getKey)))

(defn val
  "Returns the value in the map entry."
  [#^clojure.lang.IMapEntry e]  ;;  [#^java.util.Map$Entry e]
    (. e (val)))                ;; (. e (getValue)))

(defn rseq
  "Returns, in constant time, a seq of the items in rev (which
  can be a vector or sorted-map), in reverse order. If rev is empty returns nil"
  [#^clojure.lang.Reversible rev]
    (. rev (rseq)))

(defn name
  "Returns the name String of a symbol or keyword."
  {:tag String}
  [#^clojure.lang.Named x]
    (. x (getName)))

(defn namespace
  "Returns the namespace String of a symbol or keyword, or nil if not present."
  {:tag String}
  [#^clojure.lang.Named x]
    (. x (getNamespace)))
  
(defmacro locking
  "Executes exprs in an implicit do, while holding the monitor of x.
  Will release the monitor of x in all circumstances."
  [x & body]
  `(let [lockee# ~x]
     (try
      (monitor-enter lockee#)
      ~@body
      (finally
       (monitor-exit lockee#)))))

(defmacro ..
  "form => fieldName-symbol or (instanceMethodName-symbol args*)

  Expands into a member access (.) of the first member on the first
  argument, followed by the next member on the result, etc. For
  instance:

  (.. System (getProperties) (get \"os.name\"))

  expands to:

  (. (. System (getProperties)) (get \"os.name\"))

  but is easier to write, read, and understand."
  ([x form] `(. ~x ~form))
  ([x form & more] `(.. (. ~x ~form) ~@more)))
  
(defmacro ->
  "Threads the expr through the forms. Inserts x as the
  second item in the first form, making a list of it if it is not a
  list already. If there are more forms, inserts the first form as the
  second item in second form, etc."
  ([x] x)
  ([x form] (if (seq? form)
              (with-meta `(~(first form) ~x ~@(next form)) (meta form))
              (list form x)))
  ([x form & more] `(-> (-> ~x ~form) ~@more)))

(defmacro ->>
  "Threads the expr through the forms. Inserts x as the
  last item in the first form, making a list of it if it is not a
  list already. If there are more forms, inserts the first form as the
  last item in second form, etc."
  ([x form] (if (seq? form)
              (with-meta `(~(first form) ~@(next form)  ~x) (meta form))
              (list form x)))
  ([x form & more] `(->> (->> ~x ~form) ~@more)))

;;multimethods
(def global-hierarchy)

(defmacro defmulti
  "Creates a new multimethod with the associated dispatch function. 
  The docstring and attribute-map are optional.  
  
  Options are key-value pairs and may be one of:
    :default    the default dispatch value, defaults to :default
    :hierarchy  the isa? hierarchy to use for dispatching
                defaults to the global hierarchy"
  {:arglists '([name docstring? attr-map? dispatch-fn & options])}
  [mm-name & options]
  (let [docstring   (if (string? (first options))
                      (first options)
                      nil)
        options     (if (string? (first options))
                      (next options)
                      options)
        m           (if (map? (first options))
                      (first options)
                      {})
        options     (if (map? (first options))
                      (next options)
                      options)
        dispatch-fn (first options)
        options     (next options)
        ;m           (assoc m :tag 'clojure.lang.MultiFn)         ;;; ;;; ;;; Major change -- this tag is breaking my type inferencing
        m           (if docstring
                      (assoc m :doc docstring)
                      m)
        m           (if (meta mm-name)
                      (conj (meta mm-name) m)
                      m)]
    (when (= (count options) 1)
      (throw (Exception. "The syntax for defmulti has changed. Example: (defmulti name dispatch-fn :default dispatch-value)")))
    (let [options   (apply hash-map options)
          default   (get options :default :default)
          hierarchy (get options :hierarchy #'global-hierarchy)]
     `(let [v# (def ~mm-name)]
         (when-not (and (.hasRoot v#) (instance? clojure.lang.MultiFn (deref v#)))
           (def ~(with-meta mm-name m)
                (new clojure.lang.MultiFn ~(name mm-name) ~dispatch-fn ~default ~hierarchy)))))))

(defmacro defmethod
  "Creates and installs a new method of multimethod associated with dispatch-value. "
  [multifn dispatch-val & fn-tail]
  `(. ~(with-meta multifn {:tag 'clojure.lang.MultiFn}) addMethod ~dispatch-val (fn ~@fn-tail)))

(defn remove-all-methods
  "Removes all of the methods of multimethod."
 [#^clojure.lang.MultiFn multifn]
 (.reset multifn))

(defn remove-method
  "Removes the method of multimethod associated	with dispatch-value."
 [multifn dispatch-val]
 (. multifn removeMethod dispatch-val))

(defn prefer-method
  "Causes the multimethod to prefer matches of dispatch-val-x over dispatch-val-y 
   when there is a conflict"  
  [multifn dispatch-val-x dispatch-val-y]
  (. multifn preferMethod dispatch-val-x dispatch-val-y))

(defn methods
  "Given a multimethod, returns a map of dispatch values -> dispatch fns"
  [#^clojure.lang.MultiFn multifn] (.getMethodTable multifn))

(defn get-method
  "Given a multimethod and a dispatch value, returns the dispatch fn
  that would apply to that value, or nil if none apply and no default"
  [#^clojure.lang.MultiFn multifn dispatch-val] (.getMethod multifn dispatch-val))

(defn prefers
  "Given a multimethod, returns a map of preferred value -> set of other values"
  [#^clojure.lang.MultiFn multifn] (.getMethodTable multifn))
  
;;;;;;;;; var stuff

(defmacro #^{:private true} assert-args [fnname & pairs]
  `(do (when-not ~(first pairs)
         (throw (ArgumentException.                            ;;;IllegalArgumentException.
                  ~(str fnname " requires " (second pairs)))))
     ~(let [more (nnext pairs)]
        (when more
          (list* `assert-args fnname more)))))

(defmacro if-let
  "bindings => binding-form test

  If test is true, evaluates then with binding-form bound to the value of 
  test, if not, yields else"
  ([bindings then]
   `(if-let ~bindings ~then nil))
  ([bindings then else & oldform]
   (assert-args if-let
     (and (vector? bindings) (nil? oldform)) "a vector for its binding"
     (= 2 (count bindings)) "exactly 2 forms in binding vector")
   (let [form (bindings 0) tst (bindings 1)]
     `(let [temp# ~tst]
        (if temp#
          (let [~form temp#]
            ~then)
          ~else)))))

(defmacro when-let
  "bindings => binding-form test

  When test is true, evaluates body with binding-form bound to the value of test"
  [bindings & body]
  (assert-args when-let
     (vector? bindings) "a vector for its binding"
     (= 2 (count bindings)) "exactly 2 forms in binding vector")
   (let [form (bindings 0) tst (bindings 1)]
    `(let [temp# ~tst]
       (when temp#
         (let [~form temp#]
           ~@body)))))

(defn push-thread-bindings
  "WARNING: This is a low-level function. Prefer high-level macros like
  binding where ever possible.

  Takes a map of Var/value pairs. Binds each Var to the associated value for
  the current thread. Each call *MUST* be accompanied by a matching call to
  pop-thread-bindings wrapped in a try-finally!
  
      (push-thread-bindings bindings)
      (try
        ...
        (finally
          (pop-thread-bindings)))"
  [bindings]
  (clojure.lang.Var/pushThreadBindings bindings))

(defn pop-thread-bindings
  "Pop one set of bindings pushed with push-binding before. It is an error to
  pop bindings without pushing before."
  []
  (clojure.lang.Var/popThreadBindings))

(defn get-thread-bindings
  "Get a map with the Var/value pairs which is currently in effect for the
  current thread."
  []
  (clojure.lang.Var/getThreadBindings))

(defmacro binding
  "binding => var-symbol init-expr

  Creates new bindings for the (already-existing) vars, with the
  supplied initial values, executes the exprs in an implicit do, then
  re-establishes the bindings that existed before.  The new bindings
  are made in parallel (unlike let); all init-exprs are evaluated
  before the vars are bound to their new values."
  [bindings & body]
  (assert-args binding
    (vector? bindings) "a vector for its binding"
    (even? (count bindings)) "an even number of forms in binding vector")
  (let [var-ize (fn [var-vals]
                  (loop [ret [] vvs (seq var-vals)]
                    (if vvs
                      (recur  (conj (conj ret `(var ~(first vvs))) (second vvs))
                             (next (next vvs)))
                      (seq ret))))]
    `(let []
       (push-thread-bindings (hash-map ~@(var-ize bindings)))
       (try
         ~@body
         (finally
           (pop-thread-bindings))))))

(defn with-bindings*
  "Takes a map of Var/value pairs. Installs for the given Vars the associated
  values as thread-local bindings. Then calls f with the supplied arguments.
  Pops the installed bindings after f returned. Returns whatever f returns."
  [binding-map f & args]
  (push-thread-bindings binding-map)
  (try
    (apply f args)
    (finally
      (pop-thread-bindings))))

(defmacro with-bindings
  "Takes a map of Var/value pairs. Installs for the given Vars the associated
  values as thread-local bindings. The executes body. Pops the installed
  bindings after body was evaluated. Returns the value of body."
  [binding-map & body]
  `(with-bindings* ~binding-map (fn [] ~@body)))

(defn bound-fn*
  "Returns a function, which will install the same bindings in effect as in
  the thread at the time bound-fn* was called and then call f with any given
  arguments. This may be used to define a helper function which runs on a
  different thread, but needs the same bindings in place."
  [f]
  (let [bindings (get-thread-bindings)]
    (fn [& args]
      (apply with-bindings* bindings f args))))

(defmacro bound-fn
  "Returns a function defined by the given fntail, which will install the
  same bindings in effect as in the thread at the time bound-fn was called.
  This may be used to define a helper function which runs on a different
  thread, but needs the same bindings in place."
  [& fntail]
  `(bound-fn* (fn ~@fntail)))

(defn find-var
  "Returns the global var named by the namespace-qualified symbol, or
  nil if no var with that name."
 [sym] (. clojure.lang.Var (find sym)))
 
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; Refs ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn #^{:private true}
  setup-reference [#^clojure.lang.ARef r options]
  (let [opts (apply hash-map options)]
    (when (:meta opts)
      (.resetMeta r (:meta opts)))
    (when (:validator opts)
      (.setValidator r (:validator opts)))
    r))
    
(defn agent
  "Creates and returns an agent with an initial value of state and 
  zero or more options (in any order):

  :meta metadata-map

  :validator validate-fn

  :error-handler handler-fn

  :error-mode mode-keyword
  
  If metadata-map is supplied, it will be come the metadata on the
  agent. validate-fn must be nil or a side-effect-free fn of one
  argument, which will be passed the intended new state on any state
  change. If the new state is unacceptable, the validate-fn should
  return false or throw an exception.  handler-fn is called if an
  action throws an exception or if validate-fn rejects a new state --
  see set-error-handler! for details.  The mode-keyword may be either
  :continue (the default if an error-handler is given) or :fail (the
  default if no error-handler is given) -- see set-error-mode! for
  details."
  ([state & options] 
     (let [a (new clojure.lang.Agent state)
           opts (apply hash-map options)]
       (setup-reference a options)
       (when (:error-handler opts)
         (.setErrorHandler a (:error-handler opts)))
       (.setErrorMode a (or (:error-mode opts)
                            (if (:error-handler opts) :continue :fail)))
       a)))

(defn send
  "Dispatch an action to an agent. Returns the agent immediately.
  Subsequently, in a thread from a thread pool, the state of the agent
  will be set to the value of:

  (apply action-fn state-of-agent args)"
  [#^clojure.lang.Agent a f & args]
    (. a (dispatch f args false)))
    
 (defn send-off
  "Dispatch a potentially blocking action to an agent. Returns the
  agent immediately. Subsequently, in a separate thread, the state of
  the agent will be set to the value of:

  (apply action-fn state-of-agent args)"
  [#^clojure.lang.Agent a f & args]
    (. a (dispatch f args true)))

(defn release-pending-sends
  "Normally, actions sent directly or indirectly during another action
  are held until the action completes (changes the agent's
  state). This function can be used to dispatch any pending sent
  actions immediately. This has no impact on actions sent during a
  transaction, which are still held until commit. If no action is
  occurring, does nothing. Returns the number of actions dispatched."
  [] (clojure.lang.Agent/releasePendingSends))

(defn add-watch
  "Alpha - subject to change.
  Adds a watch function to an agent/atom/var/ref reference. The watch
  fn must be a fn of 4 args: a key, the reference, its old-state, its
  new-state. Whenever the reference's state might have been changed,
  any registered watches will have their functions called. The watch fn
  will be called synchronously, on the agent's thread if an agent,
  before any pending sends if agent or ref. Note that an atom's or
  ref's state may have changed again prior to the fn call, so use
  old/new-state rather than derefing the reference. Note also that watch
  fns may be called from multiple threads simultaneously. Var watchers
  are triggered only by root binding changes, not thread-local
  set!s. Keys must be unique per reference, and can be used to remove
  the watch with remove-watch, but are otherwise considered opaque by
  the watch mechanism."
  [#^clojure.lang.IRef reference key fn] (.addWatch reference key fn))

(defn remove-watch
  "Alpha - subject to change.
  Removes a watch (set by add-watch) from a reference"
  [#^clojure.lang.IRef reference key]
  (.removeWatch reference key))

(defn agent-error
  "Returns the exception thrown during an asynchronous action of the
  agent if the agent is failed.  Returns nil if the agent is not
  failed."
  [#^clojure.lang.Agent a] (.getError a))

(defn restart-agent
  "When an agent is failed, changes the agent state to new-state and
  then un-fails the agent so that sends are allowed again.  If
  a :clear-actions true option is given, any actions queued on the
  agent that were being held while it was failed will be discarded,
  otherwise those held actions will proceed.  The new-state must pass
  the validator if any, or restart will throw an exception and the
  agent will remain failed with its old state and error.  Watchers, if
  any, will NOT be notified of the new state.  Throws an exception if
  the agent is not failed."
  [#^clojure.lang.Agent a, new-state & options]
  (let [opts (apply hash-map options)]
    (.restart a new-state (if (:clear-actions opts) true false))))

(defn set-error-handler!
  "Sets the error-handler of agent a to handler-fn.  If an action
  being run by the agent throws an exception or doesn't pass the
  validator fn, handler-fn will be called with two arguments: the
  agent and the exception."
  [#^clojure.lang.Agent a, handler-fn]
  (.setErrorHandler a handler-fn))

(defn error-handler
  "Returns the error-handler of agent a, or nil if there is none.
  See set-error-handler!"
  [#^clojure.lang.Agent a]
  (.getErrorHandler a))

(defn set-error-mode!
  "Sets the error-mode of agent a to mode-keyword, which must be
  either :fail or :continue.  If an action being run by the agent
  throws an exception or doesn't pass the validator fn, an
  error-handler may be called (see set-error-handler!), after which,
  if the mode is :continue, the agent will continue as if neither the
  action that caused the error nor the error itself ever happened.
  
  If the mode is :fail, the agent will become failed and will stop
  accepting new 'send' and 'send-off' actions, and any previously
  queued actions will be held until a 'restart-agent'.  Deref will
  still work, returning the state of the agent before the error."
  [#^clojure.lang.Agent a, mode-keyword]
  (.setErrorMode a mode-keyword))

(defn error-mode
  "Returns the error-mode of agent a.  See set-error-mode!"
  [#^clojure.lang.Agent a]
  (.getErrorMode a))

(defn agent-errors
  "DEPRECATED: Use 'agent-error' instead.
  Returns a sequence of the exceptions thrown during asynchronous
  actions of the agent."
  [a]
  (when-let [e (agent-error a)]
    (list e)))

(defn clear-agent-errors
  "DEPRECATED: Use 'restart-agent' instead.
  Clears any exceptions thrown during asynchronous actions of the
  agent, allowing subsequent actions to occur."
  [#^clojure.lang.Agent a] (restart-agent a (.deref a)))

(defn shutdown-agents
  "Initiates a shutdown of the thread pools that back the agent
  system. Running actions will complete, but no new actions will be
  accepted"
  [] (. clojure.lang.Agent shutdown))
 
(defn ref
  "Creates and returns a Ref with an initial value of x and zero or
  more options (in any order):

  :meta metadata-map

  :validator validate-fn

  :min-history (default 0)
  :max-history (default 10)
  
  If metadata-map is supplied, it will be come the metadata on the
  ref. validate-fn must be nil or a side-effect-free fn of one
  argument, which will be passed the intended new state on any state
  change. If the new state is unacceptable, the validate-fn should
  return false or throw an exception. validate-fn will be called on
  transaction commit, when all refs have their final values.
  
  Normally refs accumulate history dynamically as needed to deal with
  read demands.  If you know in advance you will need a history you can
  set :min-history to ensure that it will be available when first needed (instead
  of after a read fault).   History is limited and the limit can be set
  with :max-history."
  ([x] (new clojure.lang.Ref x))
  ([x & options] 
   (let [ r #^clojure.lang.Ref (setup-reference (ref x) options)
          opts (apply hash-map options)]
    (when (:max-history opts)
       (.setMaxHistory r (:max-history opts)))  
    (when (:min-history opts)
       (.setMinHistory r (:min-history opts)))  
     r)))

(defn deref
  "Also reader macro: @ref/@agent/@var/@atom/@delay/@future. Within a transaction,
  returns the in-transaction-value of ref, else returns the
  most-recently-committed value of ref. When applied to a var, agent
  or atom, returns its current state. When applied to a delay, forces
  it if not already forced. When applied to a future, will block if
  computation not complete" 
  [#^clojure.lang.IDeref ref] (.deref ref))
 
(defn atom
  "Creates and returns an Atom with an initial value of x and zero or
  more options (in any order):

  :meta metadata-map

  :validator validate-fn

  If metadata-map is supplied, it will be come the metadata on the
  atom. validate-fn must be nil or a side-effect-free fn of one
  argument, which will be passed the intended new state on any state
  change. If the new state is unacceptable, the validate-fn should
  return false or throw an exception."
  ([x] (new clojure.lang.Atom x))
  ([x & options] (setup-reference (atom x) options)))

(defn swap!
  "Atomically swaps the value of atom to be:
  (apply f current-value-of-atom args). Note that f may be called
  multiple times, and thus should be free of side effects.  Returns
  the value that was swapped in."
  ([#^clojure.lang.Atom atom f] (.swap atom f))
  ([#^clojure.lang.Atom atom f x] (.swap atom f x))
  ([#^clojure.lang.Atom atom f x y] (.swap atom f x y))
  ([#^clojure.lang.Atom atom f x y & args] (.swap atom f x y args)))
  
(defn compare-and-set!
  "Atomically sets the value of atom to newval if and only if the
  current value of the atom is identical to oldval. Returns true if
  set happened, else false"
  [#^clojure.lang.Atom atom oldval newval] (.compareAndSet atom oldval newval))

(defn reset!
  "Sets the value of atom to newval without regard for the
  current value. Returns newval."
  [#^clojure.lang.Atom atom newval] (.reset atom newval))

(defn set-validator
  "Sets the validator-fn for a var/ref/agent/atom. validator-fn must be nil or a
  side-effect-free fn of one argument, which will be passed the intended
  new state on any state change. If the new state is unacceptable, the
  validator-fn should return false or throw an exception. If the current state (root
  value if var) is not acceptable to the new validator, an exception
  will be thrown and the validator will not be changed."
  [#^clojure.lang.IRef iref validator-fn] (. iref (setValidator validator-fn)))

(defn get-validator
  "Gets the validator-fn for a var/ref/agent/atom."
 [#^clojure.lang.IRef iref] (. iref (getValidator)))

(defn alter-meta!
  "Atomically sets the metadata for a namespace/var/ref/agent/atom to be: 

  (apply f its-current-meta args) 

  f must be free of side-effects"
 [#^clojure.lang.IReference iref f & args] (.alterMeta iref f args))

(defn reset-meta!
  "Atomically resets the metadata for a namespace/var/ref/agent/atom"
 [#^clojure.lang.IReference iref metadata-map] (.resetMeta iref metadata-map))
 
(defn commute
  "Must be called in a transaction. Sets the in-transaction-value of
  ref to:

  (apply fun in-transaction-value-of-ref args)

  and returns the in-transaction-value of ref.

  At the commit point of the transaction, sets the value of ref to be:

  (apply fun most-recently-committed-value-of-ref args)

  Thus fun should be commutative, or, failing that, you must accept
  last-one-in-wins behavior.  commute allows for more concurrency than
  ref-set."

  [#^clojure.lang.Ref ref fun & args]
    (. ref (commute fun args)))

(defn alter
  "Must be called in a transaction. Sets the in-transaction-value of
  ref to:

  (apply fun in-transaction-value-of-ref args)

  and returns the in-transaction-value of ref."
  [#^clojure.lang.Ref ref fun & args]
    (. ref (alter fun args)))

(defn ref-set
  "Must be called in a transaction. Sets the value of ref.
  Returns val."
  [#^clojure.lang.Ref ref val]
    (. ref (set val)))

(defn ref-history-count
 "Returns the history count of a ref"
 [#^clojure.lang.Ref ref]
   (.getHistoryCount ref))

(defn ref-min-history
 "Gets the min-history of a ref, or sets it and returns the ref"
 ([#^clojure.lang.Ref ref]
   (.getMinHistory ref))
 ([#^clojure.lang.Ref ref n]
   (.setMinHistory ref n)))

(defn ref-max-history
 "Gets the max-history of a ref, or sets it and returns the ref"
 ([#^clojure.lang.Ref ref]
   (.getMaxHistory ref))
 ([#^clojure.lang.Ref ref n]
   (.setMaxHistory ref n)))

(defn ensure
  "Must be called in a transaction. Protects the ref from modification
  by other transactions.  Returns the in-transaction-value of
  ref. Allows for more concurrency than (ref-set ref @ref)"
  [#^clojure.lang.Ref ref]
    (. ref (touch))
    (. ref (deref)))

(defmacro sync
  "transaction-flags => TBD, pass nil for now

  Runs the exprs (in an implicit do) in a transaction that encompasses
  exprs and any nested calls.  Starts a transaction if none is already
  running on this thread. Any uncaught exception will abort the
  transaction and flow out of sync. The exprs may be run more than
  once, but any effects on Refs will be atomic."
  [flags-ignored-for-now & body]
  `(. clojure.lang.LockingTransaction
      (runInTransaction (fn [] ~@body))))


(defmacro io!
  "If an io! block occurs in a transaction, throws an
  IllegalStateException, else runs body in an implicit do. If the
  first expression in body is a literal string, will use that as the
  exception message."
  [& body]
  (let [message (when (string? (first body)) (first body))
        body (if message (next body) body)]
    `(if (clojure.lang.LockingTransaction/isRunning)
       (throw (new InvalidOperationException ~(or message "I/O in transaction")))   ;;; IllegalStateException
       (do ~@body))))
       
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; fn stuff ;;;;;;;;;;;;;;;;


(defn comp
  "Takes a set of functions and returns a fn that is the composition
  of those fns.  The returned fn takes a variable number of args,
  applies the rightmost of fns to the args, the next
  fn (right-to-left) to the result, etc."
  ([f] f)
  ([f g] 
     (fn 
       ([] (f (g)))
       ([x] (f (g x)))
       ([x y] (f (g x y)))
       ([x y z] (f (g x y z)))
       ([x y z & args] (f (apply g x y z args)))))
  ([f g h] 
     (fn 
       ([] (f (g (h))))
       ([x] (f (g (h x))))
       ([x y] (f (g (h x y))))
       ([x y z] (f (g (h x y z))))
       ([x y z & args] (f (g (apply h x y z args))))))
  ([f1 f2 f3 & fs]
    (let [fs (reverse (list* f1 f2 f3 fs))]
      (fn [& args]
        (loop [ret (apply (first fs) args) fs (next fs)]
          (if fs
            (recur ((first fs) ret) (next fs))
            ret))))))

(defn juxt 
  "Alpha - name subject to change.
  Takes a set of functions and returns a fn that is the juxtaposition
  of those fns.  The returned fn takes a variable number of args, and
  returns a vector containing the result of applying each fn to the
  args (left-to-right).
  ((juxt a b c) x) => [(a x) (b x) (c x)]"
  ([f] 
     (fn
       ([] [(f)])
       ([x] [(f x)])
       ([x y] [(f x y)])
       ([x y z] [(f x y z)])
       ([x y z & args] [(apply f x y z args)])))
  ([f g] 
     (fn
       ([] [(f) (g)])
       ([x] [(f x) (g x)])
       ([x y] [(f x y) (g x y)])
       ([x y z] [(f x y z) (g x y z)])
       ([x y z & args] [(apply f x y z args) (apply g x y z args)])))
  ([f g h] 
     (fn
       ([] [(f) (g) (h)])
       ([x] [(f x) (g x) (h x)])
       ([x y] [(f x y) (g x y) (h x y)])
       ([x y z] [(f x y z) (g x y z) (h x y z)])
       ([x y z & args] [(apply f x y z args) (apply g x y z args) (apply h x y z args)])))
  ([f g h & fs]
     (let [fs (list* f g h fs)]
       (fn
         ([] (reduce #(conj %1 (%2)) [] fs))
         ([x] (reduce #(conj %1 (%2 x)) [] fs))
         ([x y] (reduce #(conj %1 (%2 x y)) [] fs))
         ([x y z] (reduce #(conj %1 (%2 x y z)) [] fs))
         ([x y z & args] (reduce #(conj %1 (apply %2 x y z args)) [] fs))))))

(defn partial
  "Takes a function f and fewer than the normal arguments to f, and
  returns a fn that takes a variable number of additional args. When
  called, the returned function calls f with args + additional args."
  ([f arg1]
   (fn [& args] (apply f arg1 args)))
  ([f arg1 arg2]
   (fn [& args] (apply f arg1 arg2 args)))
  ([f arg1 arg2 arg3]
   (fn [& args] (apply f arg1 arg2 arg3 args)))
  ([f arg1 arg2 arg3 & more]
   (fn [& args] (apply f arg1 arg2 arg3 (concat more args)))))

;;;;;;;;;;;;;;;;;;; sequence fns  ;;;;;;;;;;;;;;;;;;;;;;;
(defn sequence
  "Coerces coll to a (possibly empty) sequence, if it is not already
  one. Will not force a lazy seq. (sequence nil) yields ()"  
  [coll]
   (if (seq? coll) coll
    (or (seq coll) ())))

(defn every?
  "Returns true if (pred x) is logical true for every x in coll, else
  false."
  {:tag Boolean}
  [pred coll]
  (cond
   (nil? (seq coll)) true
   (pred (first coll)) (recur pred (next coll))
   :else false))

(def
 #^{:tag Boolean
    :doc "Returns false if (pred x) is logical true for every x in
  coll, else true."
    :arglists '([pred coll])}
 not-every? (comp not every?))

(defn some
  "Returns the first logical true value of (pred x) for any x in coll,
  else nil.  One common idiom is to use a set as pred, for example
  this will return :fred if :fred is in the sequence, otherwise nil:
  (some #{:fred} coll)"
  [pred coll]
    (when (seq coll)
      (or (pred (first coll)) (recur pred (next coll)))))

(def
 #^{:tag Boolean
    :doc "Returns false if (pred x) is logical true for any x in coll,
  else true."
    :arglists '([pred coll])}
 not-any? (comp not some))

;will be redefed later with arg checks
(defmacro dotimes
  "bindings => name n

  Repeatedly executes body (presumably for side-effects) with name
  bound to integers from 0 through n-1."
  [bindings & body]
  (let [i (first bindings)
        n (second bindings)]
    `(let [n# (int ~n)]
       (loop [~i (int 0)]
         (when (< ~i n#)
           ~@body
           (recur (inc ~i)))))))
           
(defn map
  "Returns a lazy sequence consisting of the result of applying f to the
  set of first items of each coll, followed by applying f to the set
  of second items in each coll, until any one of the colls is
  exhausted.  Any remaining items in other colls are ignored. Function
  f should accept number-of-colls arguments."
  ([f coll]
   (lazy-seq
    (when-let [s (seq coll)]
      (if (chunked-seq? s)
        (let [c (chunk-first s)
              size (int (count c))
              b (chunk-buffer size)]
          (dotimes [i size]
              (chunk-append b (f (.nth c i))))
          (chunk-cons (chunk b) (map f (chunk-rest s))))
        (cons (f (first s)) (map f (rest s)))))))
  ([f c1 c2]
   (lazy-seq
    (let [s1 (seq c1) s2 (seq c2)]
      (when (and s1 s2)
        (cons (f (first s1) (first s2))
              (map f (rest s1) (rest s2)))))))
  ([f c1 c2 c3]
   (lazy-seq
    (let [s1 (seq c1) s2 (seq c2) s3 (seq c3)]
      (when (and  s1 s2 s3)
        (cons (f (first s1) (first s2) (first s3))
              (map f (rest s1) (rest s2) (rest s3)))))))
  ([f c1 c2 c3 & colls]
   (let [step (fn step [cs]
                 (lazy-seq
                  (let [ss (map seq cs)]
                    (when (every? identity ss)
                      (cons (map first ss) (step (map rest ss)))))))]
     (map #(apply f %) (step (conj colls c3 c2 c1))))))

(defn mapcat
  "Returns the result of applying concat to the result of applying map
  to f and colls.  Thus function f should return a collection."
  [f & colls]
    (apply concat (apply map f colls)))
 
(defn filter
  "Returns a lazy sequence of the items in coll for which
  (pred item) returns true. pred must be free of side-effects."
  ([pred coll]
   (lazy-seq
    (when-let [s (seq coll)]
      (if (chunked-seq? s)
        (let [c (chunk-first s)
              size (count c)
              b (chunk-buffer size)]
          (dotimes [i size]
              (when (pred (.nth c i))
                (chunk-append b (.nth c i))))
          (chunk-cons (chunk b) (filter pred (chunk-rest s))))
        (let [f (first s) r (rest s)]
          (if (pred f)
            (cons f (filter pred r))
            (filter pred r))))))))
    
    
(defn remove
  "Returns a lazy sequence of the items in coll for which
  (pred item) returns false. pred must be free of side-effects."
  [pred coll]
  (filter (complement pred) coll))

(defn take
  "Returns a lazy sequence of the first n items in coll, or all items if
  there are fewer than n."
  [n coll]
  (lazy-seq
   (when (pos? n) 
     (when-let [s (seq coll)]
      (cons (first s) (take (dec n) (rest s)))))))

(defn take-while
  "Returns a lazy sequence of successive items from coll while
  (pred item) returns true. pred must be free of side-effects."
  [pred coll]
  (lazy-seq
   (when-let [s (seq coll)]
       (when (pred (first s))
         (cons (first s) (take-while pred (rest s)))))))

(defn drop
  "Returns a lazy sequence of all but the first n items in coll."
  [n coll]
  (let [step (fn [n coll]
               (let [s (seq coll)]
                 (if (and (pos? n) s)
                   (recur (dec n) (rest s))
                   s)))]
    (lazy-seq (step n coll))))

(defn drop-last
  "Return a lazy sequence of all but the last n (default 1) items in coll"
  ([s] (drop-last 1 s))
  ([n s] (map (fn [x _] x) s (drop n s))))

(defn take-last
  "Returns a seq of the last n items in coll.  Depending on the type
  of coll may be no better than linear time.  For vectors, see also subvec."
  [n coll]
  (loop [s (seq coll), lead (seq (drop n coll))]
    (if lead
      (recur (next s) (next lead))
      s)))

(defn drop-while
  "Returns a lazy sequence of the items in coll starting from the first
  item for which (pred item) returns nil."
  [pred coll]
  (let [step (fn [pred coll]
               (let [s (seq coll)]
                 (if (and s (pred (first s)))
                   (recur pred (rest s))
                   s)))]
    (lazy-seq (step pred coll))))
    
(defn cycle
  "Returns a lazy (infinite!) sequence of repetitions of the items in   coll."
  [coll] (lazy-seq 
          (when-let [s (seq coll)] 
              (concat s (cycle s)))))
              
(defn split-at
  "Returns a vector of [(take n coll) (drop n coll)]"
  [n coll]
    [(take n coll) (drop n coll)])

(defn split-with
  "Returns a vector of [(take-while pred coll) (drop-while pred coll)]"
  [pred coll]
    [(take-while pred coll) (drop-while pred coll)])

(defn repeat
  "Returns a lazy (infinite! or length n if supplied) sequence of xs."
  ([x] (lazy-seq (cons x (repeat x))))
  ([n x] (take n (repeat x))))
  
(defn replicate
  "Returns a lazy seq of n xs."
  [n x] (take n (repeat x)))

(defn iterate
  "Returns a lazy sequence of x, (f x), (f (f x)) etc. f must be free of side-effects"
  [f x] (cons x (lazy-seq (iterate f (f x)))))

(defn range 
  "Returns a lazy seq of nums from start (inclusive) to end
  (exclusive), by step, where start defaults to 0 and step to 1."
  ([end] (range 0 end 1))
  ([start end] (range start end 1))
  ([start end step]
   (lazy-seq
    (let [b (chunk-buffer 32)
          comp (if (pos? step) < >)]
      (loop [i start]
        (if (and (< (count b) 32)
                 (comp i end))
          (do
            (chunk-append b i)
            (recur (+ i step)))
          (chunk-cons (chunk b) 
                      (when (comp i end) 
                        (range i end step)))))))))

(defn merge
  "Returns a map that consists of the rest of the maps conj-ed onto
  the first.  If a key occurs in more than one map, the mapping from
  the latter (left-to-right) will be the mapping in the result."
  [& maps]
  (when (some identity maps)
    (reduce #(conj (or %1 {}) %2) maps)))

(defn merge-with
  "Returns a map that consists of the rest of the maps conj-ed onto
  the first.  If a key occurs in more than one map, the mapping(s)
  from the latter (left-to-right) will be combined with the mapping in
  the result by calling (f val-in-result val-in-latter)."
  [f & maps]
  (when (some identity maps)
    (let [merge-entry (fn [m e]
			(let [k (key e) v (val e)]
			  (if (contains? m k)
			    (assoc m k (f (m k) v)) 
			    (assoc m k v))))
          merge2 (fn [m1 m2]
		   (reduce merge-entry (or m1 {}) (seq m2)))]
      (reduce merge2 maps))))



(defn zipmap
  "Returns a map with the keys mapped to the corresponding vals."
  [keys vals]
    (loop [map {}
           ks (seq keys)
           vs (seq vals)]
      (if (and ks vs)
        (recur (assoc map (first ks) (first vs))
               (next ks)
               (next vs))
        map)))

(defmacro declare
  "defs the supplied var names with no bindings, useful for making forward declarations."
  [& names] `(do ~@(map #(list 'def (vary-meta % assoc :declared true)) names)))
  
(defn line-seq
  "Returns the lines of text from rdr as a lazy sequence of strings.
  rdr must implement java.io.BufferedReader."
  [#^System.IO.TextReader rdr ]                    ;;;  [#^java.io.BufferedReader rdr]
 (when-let [line (.ReadLine rdr)]                  ;;; readLine
    (cons line (lazy-seq (line-seq rdr)))))
        
(defn comparator
  "Returns an implementation of java.util.Comparator based upon pred."
  [pred]
    (fn [x y]
      (cond (pred x y) -1 (pred y x) 1 :else 0)))

(defn sort
  "Returns a sorted sequence of the items in coll. If no comparator is
  supplied, uses compare. comparator must
  implement java.util.Comparator."
  ([coll]
   (sort compare coll))
  ([comp coll]     ;;;   We can't pass in a Comparator directly at this point, only a ClojureRuntimeDelegate :  [#^java.util.Comparator comp coll]
   (if (seq coll)
     (let [a (to-array coll)]
       (. clojure.lang.RT (SortArray a comp))   ;;; see above: (. java.util.Arrays (sort a comp))
       (seq a))
     ())))

(defn sort-by
  "Returns a sorted sequence of the items in coll, where the sort
  order is determined by comparing (keyfn item).  If no comparator is
  supplied, uses compare. comparator must
  implement java.util.Comparator."
  ([keyfn coll]
   (sort-by keyfn compare coll))
  ([keyfn comp coll]   ;;; --- Can't pass a Comparator directly: [keyfn #^java.util.Comparator comp coll]
   (sort (fn [x y] (comp (keyfn x) (keyfn y))) coll)))  ;;;(sort (fn [x y] (. comp (compare (keyfn x) (keyfn y)))) coll)))

(defn partition
  "Returns a lazy sequence of lists of n items each, at offsets step
  apart. If step is not supplied, defaults to n, i.e. the partitions
  do not overlap. If a pad collection is supplied, use its elements as
  necessary to complete last partition upto n items. In case there are
  not enough padding elements, return a partition with less than n items."
  ([n coll]
     (partition n n coll))
  ([n step coll]
     (lazy-seq
       (when-let [s (seq coll)]
         (let [p (take n s)]
           (when (= n (count p))
             (cons p (partition n step (drop step s))))))))
  ([n step pad coll]
     (lazy-seq
       (when-let [s (seq coll)]
         (let [p (take n s)]
           (if (= n (count p))
             (cons p (partition n step pad (drop step s)))
             (list (take n (concat p pad)))))))))

;; evaluation

(defn eval
  "Evaluates the form data structure (not text!) and returns the result."
  [form] (. clojure.lang.Compiler (eval form)))

(defmacro doseq
  "Repeatedly executes body (presumably for side-effects) with
  bindings and filtering as provided by \"for\".  Does not retain
  the head of the sequence. Returns nil."
  [seq-exprs & body]
  (assert-args doseq
     (vector? seq-exprs) "a vector for its binding"
     (even? (count seq-exprs)) "an even number of forms in binding vector")
  (let [step (fn step [recform exprs]
               (if-not exprs
                 [true `(do ~@body)]
                 (let [k (first exprs)
                       v (second exprs)]
                   (if (keyword? k)
                     (let [steppair (step recform (nnext exprs))
                           needrec (steppair 0)
                           subform (steppair 1)]
                       (cond
                         (= k :let) [needrec `(let ~v ~subform)]
                         (= k :while) [false `(when ~v
                                                ~subform
                                                ~@(when needrec [recform]))]
                         (= k :when) [false `(if ~v
                                               (do
                                                 ~subform
                                                 ~@(when needrec [recform]))
                                               ~recform)]))
                     (let [seq- (gensym "seq_")
                           chunk- (with-meta (gensym "chunk_")
                                             {:tag 'clojure.lang.IChunk})
                           count- (gensym "count_")
                           i- (gensym "i_")
                           recform `(recur (next ~seq-) nil (int 0) (int 0))
                           steppair (step recform (nnext exprs))
                           needrec (steppair 0)
                           subform (steppair 1)
                           recform-chunk 
                             `(recur ~seq- ~chunk- ~count- (unchecked-inc ~i-))
                           steppair-chunk (step recform-chunk (nnext exprs))
                           subform-chunk (steppair-chunk 1)]
                       [true
                        `(loop [~seq- (seq ~v), ~chunk- nil,
                                ~count- (int 0), ~i- (int 0)]
                           (if (< ~i- ~count-)
                             (let [~k (.nth ~chunk- ~i-)]
                               ~subform-chunk
                               ~@(when needrec [recform-chunk]))
                             (when-let [~seq- (seq ~seq-)]
                               (if (chunked-seq? ~seq-)
                                 (let [c# (chunk-first ~seq-)]
                                   (recur (chunk-rest ~seq-) c#
                                          (int (count c#)) (int 0)))
                                 (let [~k (first ~seq-)]
                                   ~subform
                                   ~@(when needrec [recform]))))))])))))]
    (nth (step nil (seq seq-exprs)) 1)))

(defn dorun
  "When lazy sequences are produced via functions that have side
  effects, any effects other than those needed to produce the first
  element in the seq do not occur until the seq is consumed. dorun can
  be used to force any effects. Walks through the successive nexts of
  the seq, does not retain the head and returns nil."
  ([coll]
   (when (seq coll)
     (recur (next coll))))
  ([n coll]
   (when (and (seq coll) (pos? n))
     (recur (dec n) (next coll)))))

(defn doall
  "When lazy sequences are produced via functions that have side
  effects, any effects other than those needed to produce the first
  element in the seq do not occur until the seq is consumed. doall can
  be used to force any effects. Walks through the successive nexts of
  the seq, retains the head and returns it, thus causing the entire
  seq to reside in memory at one time."
  ([coll]
   (dorun coll)
   coll)
  ([n coll]
   (dorun n coll)
   coll))

(defn await
  "Blocks the current thread (indefinitely!) until all actions
  dispatched thus far, from this thread or agent, to the agent(s) have
  occurred.  Will block on failed agents.  Will never return if
  a failed agent is restarted with :clear-actions true."
  [& agents]
  (io! "await in transaction"
    (when *agent*
      (throw (new Exception "Can't await in agent action")))
    (let [latch (new clojure.lang.CountDownLatch (count agents))  ;;; java.util.concurrent.CountDownLatch
          count-down (fn [agent] (. latch (CountDown)) agent)]    ;;; countDown
      (doseq [agent agents]
        (send agent count-down))
      (. latch (Await)))))                                        ;;; await

(defn await1 [#^clojure.lang.Agent a]
  (when (pos? (.getQueueCount a))
    (await a))
    a)

(defn await-for
  "Blocks the current thread until all actions dispatched thus
  far (from this thread or agent) to the agents have occurred, or the
  timeout (in milliseconds) has elapsed. Returns nil if returning due
  to timeout, non-nil otherwise."
  [timeout-ms & agents]
    (io! "await-for in transaction"
     (when *agent*
       (throw (new Exception "Can't await in agent action")))
     (let [latch (new clojure.lang.CountDownLatch (count agents))   ;;; java.util.concurrent.CountDownLatch
           count-down (fn [agent] (. latch (CountDown)) agent)]     ;;; countDown
       (doseq [agent agents]
           (send agent count-down))
       (. latch (Await timeout-ms)))))   ;;;(await  timeout-ms (. java.util.concurrent.TimeUnit MILLISECONDS))))))

(defmacro dotimes
  "bindings => name n

  Repeatedly executes body (presumably for side-effects) with name
  bound to integers from 0 through n-1."
  [bindings & body]
  (assert-args dotimes
     (vector? bindings) "a vector for its binding"
     (= 2 (count bindings)) "exactly 2 forms in binding vector")
  (let [i (first bindings)
        n (second bindings)]
    `(let [n# (int ~n)]
       (loop [~i (int 0)]
         (when (< ~i n#)
           ~@body
           (recur (unchecked-inc ~i)))))))
  
(defn into
  "Returns a new coll consisting of to-coll with all of the items of
  from-coll conjoined."
  [to from]
    (let [ret to items (seq from)]
      (if items
        (recur (conj ret (first items)) (next items))
        ret)))

(defmacro import 
  "import-list => (package-symbol class-name-symbols*)

  For each name in class-name-symbols, adds a mapping from name to the
  class named by package.name to the current namespace. Use :import in the ns
  macro in preference to calling this directly."
  [& import-symbols-or-lists]
  (let [specs (map #(if (and (seq? %) (= 'quote (first %))) (second %) %) 
                   import-symbols-or-lists)]
    `(do ~@(map #(list 'clojure.core/import* %)
                (reduce (fn [v spec] 
                          (if (symbol? spec)
                            (conj v (name spec))
                            (let [p (first spec) cs (rest spec)]
                              (into v (map #(str p "." %) cs)))))
                        [] specs)))))

(defn into-array
  "Returns an array with components set to the values in aseq. The array's
  component type is type if provided, or the type of the first value in
  aseq if present, or Object. All values in aseq must be compatible with
  the component type. Class objects for the primitive types can be obtained
  using, e.g., Integer/TYPE."
  ([aseq]
     (clojure.lang.RT/seqToTypedArray (seq aseq)))
  ([type aseq]
     (clojure.lang.RT/seqToTypedArray type (seq aseq))))

(defn #^{:private true}
  array [& items]
    (into-array items))

(defn #^Type class               ;;;#^Class class
  "Returns the Class of x"
  [#^Object x] (if (nil? x) x (. x (GetType))))  ;;; getClass => GetType
  
(defn type 
  "Returns the :type metadata of x, or its Class if none"
  [x]
  (or (:type (meta x)) (class x)))

(defn num
  "Coerce to Number"
  {:tag Object                                           ;;; Number
  }                               ;;;  :inline (fn  [x] `(. clojure.lang.Numbers (num ~x)))}
  [x] x)                          ;;;  (. clojure.lang.Numbers (num x)))

  (defn long
  "Coerce to long"
  {:tag Int64    ;;; Long
   :inline (fn  [x] `(. clojure.lang.RT (longCast ~x)))}
  [x] (. clojure.lang.RT (longCast x)))  ;;;    [#^Number x] (. x (longValue)))
  
(defn float
  "Coerce to float"
  {:tag Single   ;;; Float
   :inline (fn  [x] `(. clojure.lang.RT (floatCast ~x)))}
  [x] (. clojure.lang.RT (floatCast x)))  ;;; [#^Number x] (. x (floatValue)))

(defn double
  "Coerce to double"
  {:tag Double
   :inline (fn  [x] `(. clojure.lang.RT (doubleCast ~x)))}
  [x] (. clojure.lang.RT (doubleCast x)))  ;;;   [#^Number x] (. x (doubleValue)))

(defn short
  "Coerce to short"
  {:tag Int16 
   :inline (fn  [x] `(. clojure.lang.RT (shortCast ~x)))}
  [x] (. clojure.lang.RT (shortCast x)))  ;;;   [#^Number x] (. x (shortValue)))

(defn byte
  "Coerce to byte"
  {:tag SByte                                                                 ;;; Byte
   :inline (fn  [x] `(. clojure.lang.RT (byteCast ~x)))}
  [x] (. clojure.lang.RT (byteCast x)))  ;;;   [#^Number x] (. x (byteValue)))

(defn char
  "Coerce to char"
  {:tag Char    ;;; Character
   :inline (fn  [x] `(. clojure.lang.RT (charCast ~x)))}
  [x] (. clojure.lang.RT (charCast x)))  

(defn boolean
  "Coerce to boolean"
  {:tag Boolean
   :inline (fn  [x] `(. clojure.lang.RT (booleanCast ~x)))}
  [x] (if x true false))

(defn number?
  "Returns true if x is a Number"
  [x]
  (. clojure.lang.Util (IsNumeric x)))    ;;; (instance? Number x))
;;; Should we include th other numeric types in the CLR (unsigned, etc.)
(defn integer?
  "Returns true if n is an integer"
  [n]
  (or (instance? Int32 n)      (instance? UInt32 n)     ;;; Integer -> Int32, added UInt32
      (instance? Int64 n)      (instance? UInt64 n)     ;;; Long -> Int64, added UInt64
      (instance? BigInteger n) (instance? Char n)       ;;; added Char test
      (instance? Int16 n)      (instance? UInt16 n)     ;;; Short -> Int16, added UInt16
      (instance? Byte n)  (instance? SByte n)))         ;;; Added SByte test
      
(defn mod
  "Modulus of num and div. Truncates toward negative infinity." 
  [num div] 
  (let [m (rem num div)] 
    (if (or (zero? m) (pos? (* num div))) 
      m 
      (+ m div))))

(defn ratio?
  "Returns true if n is a Ratio"
  [n] (instance? clojure.lang.Ratio n))

(defn numerator
 "Returns the numerator part of a Ratio."
 {:tag BigInteger}
 [r]
  (.numerator #^clojure.lang.Ratio r))

(defn denominator
 "Returns the denominator part of a Ratio."
 {:tag BigInteger}
 [r]
  (.denominator #^clojure.lang.Ratio r))

(defn decimal?
  "Returns true if n is a BigDecimal"
  [n] (instance? BigDecimal n))

(defn float?
  "Returns true if n is a floating point number"
  [n]   
  (or (instance? Double n)
      (instance? Single n)))     ;;; Float

(defn rational? [n]
  "Returns true if n is a rational number"
  (or (integer? n) (ratio? n) (decimal? n)))

(defn bigint
  "Coerce to BigInteger"
  {:tag BigInteger}
  [x] (cond
       (instance? BigInteger x) x
       (decimal? x) (.ToBigInteger #^BigDecimal x)             ;;; toBigInteger
       (ratio? x) (.BigIntegerValue #^clojure.lang.Ratio x)
       (number? x) (BigInteger/Create (long x))                ;;;(BigInteger/valueOf (long x))
       :else (BigInteger. x)))

(defn bigdec
  "Coerce to BigDecimal"
  {:tag BigDecimal}
  [x] (cond
       (decimal? x) x
       (float? x) (BigDecimal/Create (double x))                     ;;; (. BigDecimal valueOf (double x))
       (ratio? x) (.ToBigDecimal #^clojure.lang.Ratio x)             ;;; (/ (BigDecimal. (.numerator x)) (.denominator x))
       (instance? BigInteger x) (BigDecimal/Create #^BigInteger x)   ;;; (BigDecimal. #^BigInteger x)
       (number? x) (BigDecimal/Create (long x))                      ;;; (BigDecimal/valueOf (long x))
       :else  (BigDecimal/Create x)))                                ;;; (BigDecimal. x)))

(def #^{:private true} print-initialized false)

(defmulti print-method (fn [x writer] (type x)))
(defmulti print-dup (fn [x writer] (class x)))

(defn pr-on
  {:private true}
  [x w]
  (if *print-dup*
    (print-dup x w)
    (print-method x w))
  nil)

(defn pr
  "Prints the object(s) to the output stream that is the current value
  of *out*.  Prints the object(s), separated by spaces if there is
  more than one.  By default, pr and prn print in a way that objects
  can be read by the reader"
  {:dynamic true}
  ([] nil)
  ([x]
     (pr-on x *out*))
  ([x & more]
   (pr x)
   (. *out* (Write \space))  ;; append -> Write
   (apply pr more)))
   
(defn newline
  "Writes a newline to the output stream that is the current value of
  *out*"
  []
    (. *out* (Write \newline))  ;; append -> Write
    nil)

(defn flush 
  "Flushes the output stream that is the current value of
  *out*"
  []
    (. *out* (Flush))             ;; flush => Flush
    nil)

(defn prn
  "Same as pr followed by (newline). Observes *flush-on-newline*"
  [& more]
    (apply pr more)
    (newline)
    (when *flush-on-newline*
      (flush)))

(defn print
  "Prints the object(s) to the output stream that is the current value
  of *out*.  print and println produce output for human consumption."
  [& more]
    (binding [*print-readably* nil]
      (apply pr more)))

(defn println
  "Same as print followed by (newline)"
  [& more]
    (binding [*print-readably* nil]
      (apply prn more)))


(defn read   ;;; still have an error here, probably from leftover newline causing interference with REPL
  "Reads the next object from stream, which must be an instance of
  java.io.PushbackReader or some derivee.  stream defaults to the
  current value of *in* ."
  ([]
   (read *in*))
  ([stream]
   (read stream true nil))
  ([stream eof-error? eof-value]
   (read stream eof-error? eof-value false))
  ([stream eof-error? eof-value recursive?]
   (. clojure.lang.LispReader (read stream (boolean eof-error?) eof-value recursive?))))

(defn read-line  
  "Reads the next line from stream that is the current value of *in* ."
  [] (.ReadLine #^System.IO.TextReader *in* ))   ;;; readLine => ReadLine     #^java.io.BufferedReader 
;;;  (if (instance? clojure.lang.LineNumberingPushbackReader *in*)
;;;    (.readLine #^clojure.lang.LineNumberingPushbackReader *in*)
;;;    (.readLine #^java.io.BufferedReader *in*)))
    
(defn read-string
  "Reads one object from the string s"
  [s] (clojure.lang.RT/readString s))

(defn subvec
  "Returns a persistent vector of the items in vector from
  start (inclusive) to end (exclusive).  If end is not supplied,
  defaults to (count vector). This operation is O(1) and very fast, as
  the resulting vector shares structure with the original and no
  trimming is done."
  ([v start]
   (subvec v start (count v)))
  ([v start end]
   (. clojure.lang.RT (subvec v start end))))
   
(defmacro with-open
  "bindings => name init

  Evaluates body in a try expression with names bound to the values
  of the inits, and a finally clause that calls (.close name) on each
  name in reverse order."
  [bindings & body]
  (assert-args with-open
     (vector? bindings) "a vector for its binding"
     (even? (count bindings)) "an even number of forms in binding vector")
  (cond
    (= (count bindings) 0) `(do ~@body)
    (symbol? (bindings 0)) `(let ~(subvec bindings 0 2)
                              (try
                                (with-open ~(subvec bindings 2) ~@body)
                                (finally
                                  (. ~(bindings 0) Dispose))))      ;;; close => Dispose
    :else (throw (ArgumentException.                              ;;;IllegalArgumentException.
                   "with-open only allows Symbols in bindings"))))

(defmacro doto
  "Evaluates x then calls all of the methods and functions with the
  value of x supplied at the from of the given arguments.  The forms
  are evaluated in order.  Returns x.

  (doto (new java.util.HashMap) (.put \"a\" 1) (.put \"b\" 2))"
  [x & forms]
    (let [gx (gensym)]
      `(let [~gx ~x]
         ~@(map (fn [f]
                  (if (seq? f)
                    `(~(first f) ~gx ~@(next f))
                    `(~f ~gx)))
                forms)
         ~gx)))

(defmacro memfn
  "Expands into code that creates a fn that expects to be passed an
  object and any args and calls the named instance method on the
  object passing the args. Use when you want to treat a Java method as
  a first-class fn."
  [name & args]
  `(fn [target# ~@args]
     (. target# (~name ~@args))))

(defmacro time    
  "Evaluates expr and prints the time it took.  Returns the value of
 expr."
  [expr]
  `(let [start# (. clojure.lang.RT (StartStopwatch))   ;;; (. System (nanoTime))       
         ret# ~expr]
     (prn (str "Elapsed time: " (. clojure.lang.RT StopStopwatch) " msecs"))     ;;;(/ (double (- (. System (nanoTime)) start#)) 1000000.0) " msecs"))
     ret#))



;;; Java version has: (import '(java.lang.reflect Array))

(defn alength
  "Returns the length of the Java array. Works on arrays of all
  types."
  {:inline (fn [a] `(. clojure.lang.RT (alength ~a)))}
  [array] (. clojure.lang.RT (alength array)))

(defn aclone
  "Returns a clone of the Java array. Works on arrays of known
  types."
  {:inline (fn [a] `(. clojure.lang.RT (aclone ~a)))}
  [array] (. clojure.lang.RT (aclone array)))
;;; We have a real problem with aget/aset -- Java has only single dim arrays, CLR has true multidim.  How to distinguish true multidim from ragged?  For now, treat all as ragged. 
(defn aget
  "Returns the value at the index/indices. Works on Java arrays of all
  types."
  {:inline (fn [a i] `(. clojure.lang.RT (aget ~a (int ~i))))
   :inline-arities #{2}}
  ([array idx]
   (clojure.lang.Reflector/prepRet (. array (GetValue idx))))  ;;; was  (. Array (get array idx)))  
  ([array idx & idxs]
   (apply aget (aget array idx) idxs)))

(defn aset
  "Sets the value at the index/indices. Works on Java arrays of
  reference types. Returns val."
  {:inline (fn [a i v] `(. clojure.lang.RT (aset ~a (int ~i) ~v)))
   :inline-arities #{3}}
  ([array idx val]
   (. array (SetValue val idx))  ;;; was     (. Array (set array idx val))
   val)
  ([array idx idx2 & idxv]
   (apply aset (aget array idx) idx2 idxv)))

(defmacro
  #^{:private true}
  def-aset [name method coerce]
    `(defn ~name
       {:arglists '([~'array ~'idx ~'val] [~'array ~'idx ~'idx2 & ~'idxv])}
       ([array# idx# val#]
        (. clojure.lang.ArrayHelper (~method array# idx# (~coerce val#)))        ;;; Array -> ArrayHelper so we can provide the overloads below.
        val#)
       ([array# idx# idx2# & idxv#]
        (apply ~name (aget array# idx#) idx2# idxv#))))

(def-aset
  #^{:doc "Sets the value at the index/indices. Works on arrays of int. Returns val."}
  aset-int setInt int)

(def-aset
  #^{:doc "Sets the value at the index/indices. Works on arrays of long. Returns val."}
  aset-long setLong long)

(def-aset
  #^{:doc "Sets the value at the index/indices. Works on arrays of boolean. Returns val."}
  aset-boolean setBoolean boolean)

(def-aset
  #^{:doc "Sets the value at the index/indices. Works on arrays of float. Returns val."}
  aset-float setFloat float)

(def-aset
  #^{:doc "Sets the value at the index/indices. Works on arrays of double. Returns val."}
  aset-double setDouble double)

(def-aset
  #^{:doc "Sets the value at the index/indices. Works on arrays of short. Returns val."}
  aset-short setShort short)

(def-aset
  #^{:doc "Sets the value at the index/indices. Works on arrays of byte. Returns val."}
  aset-byte setByte byte)

(def-aset
  #^{:doc "Sets the value at the index/indices. Works on arrays of char. Returns val."}
  aset-char setChar char)
;;; Another  ragged versus true multidimensional array problem -- we will go with ragged here so as not to break aget/aset
(defn make-array
  "Creates and returns an array of instances of the specified class of
  the specified dimension(s).  Note that a class object is required.
  Class objects can be obtained by using their imported or
  fully-qualified name.  Class objects for the primitive types can be
  obtained using, e.g., Integer/TYPE."
  ([#^Type type len]                                                     ;;; #^Class
   (. Array (CreateInstance type (int len))))                            ;;; newInstance
  ([#^Type type dim & more-dims]                                        ;;; #^Class
   (let [ a  (. Array (CreateInstance Array (int dim)))]       ;;;    [dims (cons dim more-dims)
                                                               ;;;     #^"[I" dimarray (make-array (. Integer TYPE)  (count dims))]
      (dotimes [i dim]                                         ;;;       (dotimes [i (alength dimarray)]
          (aset a i (apply make-array type more-dims)))        ;;;   (aset-int dimarray i (nth dims i)))
      a)))                                                     ;;; (. Array (newInstance type dimarray)))))

(defn to-array-2d
  "Returns a (potentially-ragged) 2-dimensional array of Objects
  containing the contents of coll, which can be any Collection of any
  Collection."
  {:tag "System.Object[]" }                                                                 ;;; "[[Ljava.lang.Object;"
  [#^System.Collections.ICollection coll]                                              ;;; #^java.util.Collection
    (let [ret  (make-array Object (.Count coll))]      ;;; NEED BETTER TYPING HERE (make-array (. Class (forName "[Ljava.lang.Object;")) (. coll (size)))]
      (loop [i 0 xs (seq coll)]
        (when xs
          (aset ret i (to-array (first xs)))
          (recur (inc i) (next xs))))
      ret))

(defn macroexpand-1
  "If form represents a macro form, returns its expansion,
  else returns form."
  [form]
    (. clojure.lang.Compiler (macroexpand1 form)))

(defn macroexpand
  "Repeatedly calls macroexpand-1 on form until it no longer
  represents a macro form, then returns it.  Note neither
  macroexpand-1 nor macroexpand expand macros in subforms."
  [form]
    (let [ex (macroexpand-1 form)]
      (if (identical? ex form)
        form
        (macroexpand ex))))

(defn create-struct
  "Returns a structure basis object."
  [& keys]
    (. clojure.lang.PersistentStructMap (createSlotMap keys)))

(defmacro defstruct
  "Same as (def name (create-struct keys...))"
  [name & keys]
  `(def ~name (create-struct ~@keys)))
 
(defn struct-map
  "Returns a new structmap instance with the keys of the
  structure-basis. keyvals may contain all, some or none of the basis
  keys - where values are not supplied they will default to nil.
  keyvals can also contain keys not in the basis."
  [s & inits]
    (. clojure.lang.PersistentStructMap (create s inits)))

(defn struct
  "Returns a new structmap instance with the keys of the
  structure-basis. vals must be supplied for basis keys in order -
  where values are not supplied they will default to nil."
  [s & vals]
    (. clojure.lang.PersistentStructMap (construct s vals)))

(defn accessor
  "Returns a fn that, given an instance of a structmap with the basis,
  returns the value at the key.  The key must be in the basis. The
  returned function should be (slightly) more efficient than using
  get, but such use of accessors should be limited to known
  performance-critical areas."
  [s key]
    (. clojure.lang.PersistentStructMap (getAccessor s key)))
 
(defn load-reader
  "Sequentially read and evaluate the set of forms contained in the
  stream/file"
  [rdr] (. clojure.lang.Compiler (load rdr)))

(defn load-string  ;;; EOF problem here.
  "Sequentially read and evaluate the set of forms contained in the
  string"
  [s]
  (let [rdr (-> (System.IO.StringReader. s)     ;;; was (java.io.StringReader. s)
                (clojure.lang.LineNumberingTextReader.))]   ;;; was (clojure.lang.LineNumberingPushbackReader.))]
    (load-reader rdr)))

(defn set
  "Returns a set of the distinct elements of coll."
  [coll] (clojure.lang.PersistentHashSet/create #^clojure.lang.ISeq (seq coll)))

(defn #^{:private true}
  filter-key [keyfn pred amap]
    (loop [ret {} es (seq amap)]
      (if es
        (if (pred (keyfn (first es)))
          (recur (assoc ret (key (first es)) (val (first es))) (next es))
          (recur ret (next es)))
        ret)))

(defn find-ns
  "Returns the namespace named by the symbol or nil if it doesn't exist."
  [sym] (clojure.lang.Namespace/find sym))

(defn create-ns
  "Create a new namespace named by the symbol if one doesn't already
  exist, returns it or the already-existing namespace of the same
  name."
  [sym] (clojure.lang.Namespace/findOrCreate sym))

(defn remove-ns
  "Removes the namespace named by the symbol. Use with caution.
  Cannot be used to remove the clojure namespace."
  [sym] (clojure.lang.Namespace/remove sym))

(defn all-ns
  "Returns a sequence of all namespaces."
  [] (clojure.lang.Namespace/all))

(defn #^clojure.lang.Namespace the-ns 
  "If passed a namespace, returns it. Else, when passed a symbol,
  returns the namespace named by it, throwing an exception if not
  found."
  [x]
  (if (instance? clojure.lang.Namespace x)
    x
    (or (find-ns x) (throw (Exception. (str "No namespace: " x " found"))))))

(defn ns-name
  "Returns the name of the namespace, a symbol."
  [ns]
  (.getName (the-ns ns)))

(defn ns-map
  "Returns a map of all the mappings for the namespace."
  [ns]
  (.getMappings (the-ns ns)))

(defn ns-unmap
  "Removes the mappings for the symbol from the namespace."
  [ns sym]
  (.unmap (the-ns ns) sym))
; commented out in Java original
;(defn export [syms]
;  (doseq [sym syms]
;   (.. *ns* (intern sym) (setExported true))))

(defn ns-publics
  "Returns a map of the public intern mappings for the namespace."
  [ns]
  (let [ns (the-ns ns)]
    (filter-key val (fn [ v] (and (instance? clojure.lang.Var v)    ;;;  removed the tag on v:  #^clojure.lang.Var
                                 (= ns (.ns v))
                                 (.isPublic v)))
                (ns-map ns))))

(defn ns-imports
  "Returns a map of the import mappings for the namespace."
  [ns]
  (filter-key val (partial instance? Type) (ns-map ns)))   ;;; Class => Type

(defn refer
  "refers to all public vars of ns, subject to filters.
  filters can include at most one each of:

  :exclude list-of-symbols
  :only list-of-symbols
  :rename map-of-fromsymbol-tosymbol

  For each public interned var in the namespace named by the symbol,
  adds a mapping from the name of the var to the var to the current
  namespace.  Throws an exception if name is already mapped to
  something else in the current namespace. Filters can be used to
  select a subset, via inclusion or exclusion, or to provide a mapping
  to a symbol different from the var's name, in order to prevent
  clashes. Use :use in the ns macro in preference to calling this directly."
  [ns-sym & filters]   
    (let [ns (or (find-ns ns-sym) (throw (new Exception (str "No namespace: " ns-sym))))   
          fs (apply hash-map filters)
          nspublics (ns-publics ns)
          rename (or (:rename fs) {})
          exclude (set (:exclude fs))
          to-do (or (:only fs) (keys nspublics))]
      (doseq [sym to-do]
        (when-not (exclude sym)
          (let [v (nspublics sym)]
            (when-not v
              (throw (new InvalidOperationException (str sym " is not public"))))    ;;; java.lang.IllegalAccessError  ==> InvalidOperationException
            (. *ns* (refer (or (rename sym) sym) v)))))))

(defn ns-refers
  "Returns a map of the refer mappings for the namespace."
  [ns]
  (let [ns (the-ns ns)]
    (filter-key val (fn [#^clojure.lang.Var v] (and (instance? clojure.lang.Var v)
                                 (not= ns (.ns v))))
                (ns-map ns))))

(defn ns-interns
  "Returns a map of the intern mappings for the namespace."
  [ns]
  (let [ns (the-ns ns)]
    (filter-key val (fn [#^clojure.lang.Var v] (and (instance? clojure.lang.Var v)
                                 (= ns (.ns v))))
                (ns-map ns))))

(defn alias
  "Add an alias in the current namespace to another
  namespace. Arguments are two symbols: the alias to be used, and
  the symbolic name of the target namespace. Use :as in the ns macro in preference
  to calling this directly."
  [alias namespace-sym]
  (.addAlias *ns* alias (find-ns namespace-sym)))

(defn ns-aliases
  "Returns a map of the aliases for the namespace."
  [ns]
  (.getAliases (the-ns ns)))

(defn ns-unalias
  "Removes the alias for the symbol from the namespace."
  [ns sym]
  (.removeAlias (the-ns ns) sym))

(defn take-nth
  "Returns a lazy seq of every nth item in coll."
  [n coll]
    (lazy-seq
     (when-let [s (seq coll)]
       (cons (first s) (take-nth n (drop n s))))))

(defn interleave
  "Returns a lazy seq of the first item in each coll, then the second etc."
  ([c1 c2]
     (lazy-seq
      (let [s1 (seq c1) s2 (seq c2)]
        (when (and s1 s2)
          (cons (first s1) (cons (first s2) 
                                 (interleave (rest s1) (rest s2))))))))
  ([c1 c2 & colls] 
     (lazy-seq 
      (let [ss (map seq (conj colls c2 c1))]
        (when (every? identity ss)
          (concat (map first ss) (apply interleave (map rest ss))))))))

(defn var-get
  "Gets the value in the var object"
  [#^clojure.lang.Var x] (. x (get)))

(defn var-set
  "Sets the value in the var object to val. The var must be
 thread-locally bound."
  [#^clojure.lang.Var x val] (. x (set val)))

(defmacro with-local-vars
  "varbinding=> symbol init-expr

  Executes the exprs in a context in which the symbols are bound to
  vars with per-thread bindings to the init-exprs.  The symbols refer
  to the var objects themselves, and must be accessed with var-get and
  var-set"
  [name-vals-vec & body]
  (assert-args with-local-vars
     (vector? name-vals-vec) "a vector for its binding"
     (even? (count name-vals-vec)) "an even number of forms in binding vector")
  `(let [~@(interleave (take-nth 2 name-vals-vec)
                       (repeat '(. clojure.lang.Var (create))))]
     (. clojure.lang.Var (pushThreadBindings (hash-map ~@name-vals-vec)))
     (try
      ~@body
      (finally (. clojure.lang.Var (popThreadBindings))))))

(defn ns-resolve
  "Returns the var or Class to which a symbol will be resolved in the
  namespace, else nil.  Note that if the symbol is fully qualified,
  the var/Class to which it resolves need not be present in the
  namespace."
  [ns sym]
  (clojure.lang.Compiler/maybeResolveIn (the-ns ns) sym))

(defn resolve
  "same as (ns-resolve *ns* symbol)"
  [sym] (ns-resolve *ns* sym))

(defn array-map
  "Constructs an array-map."
  ([] (. clojure.lang.PersistentArrayMap EMPTY))
  ([& keyvals] (clojure.lang.PersistentArrayMap/createWithCheck (to-array keyvals))))

(defn nthnext
  "Returns the nth next of coll, (seq coll) when n is 0."
  [coll n]
    (loop [n n xs (seq coll)]
      (if (and xs (pos? n))
        (recur (dec n) (next xs))
        xs)))


;redefine let and loop  with destructuring
(defn destructure [bindings]
  (let [bents (partition 2 bindings)
        pb (fn pb [bvec b v]
               (let [pvec
                     (fn [bvec b val]
                       (let [gvec (gensym "vec__")]
                         (loop [ret (-> bvec (conj gvec) (conj val))
                                n 0
                                bs b
                                seen-rest? false]
                           (if (seq bs)
                             (let [firstb (first bs)]
                               (cond
                                (= firstb '&) (recur (pb ret (second bs) (list `nthnext gvec n))
                                                     n
                                                     (nnext bs)
                                                     true)
                                (= firstb :as) (pb ret (second bs) gvec)
                                :else (if seen-rest?
                                        (throw (new Exception "Unsupported binding form, only :as can follow & parameter"))
                                        (recur (pb ret firstb  (list `nth gvec n nil))
                                               (inc n)
                                               (next bs)
                                               seen-rest?))))
                             ret))))
                     pmap
                     (fn [bvec b v]
                       (let [gmap (or (:as b) (gensym "map__"))
                             defaults (:or b)]
                         (loop [ret (-> bvec (conj gmap) (conj v)
                                        (conj gmap) (conj `(if (seq? ~gmap) (apply hash-map ~gmap) ~gmap)))
                                bes (reduce
                                     (fn [bes entry]
                                       (reduce #(assoc %1 %2 ((val entry) %2))
                                               (dissoc bes (key entry))
                                               ((key entry) bes)))
                                     (dissoc b :as :or)
                                     {:keys #(keyword (str %)), :strs str, :syms #(list `quote %)})]
                           (if (seq bes)
                             (let [bb (key (first bes))
                                   bk (val (first bes))
                                   has-default (contains? defaults bb)]
                               (recur (pb ret bb (if has-default
                                                   (list `get gmap bk (defaults bb))
                                                   (list `get gmap bk)))
                                      (next bes)))
                             ret))))]
                 (cond
                  (symbol? b) (-> bvec (conj b) (conj v))
                  (vector? b) (pvec bvec b v)
                  (map? b) (pmap bvec b v)
                  :else (throw (new Exception (str "Unsupported binding form: " b))))))
        process-entry (fn [bvec b] (pb bvec (first b) (second b)))]
    (if (every? symbol? (map first bents))
      bindings
      (reduce process-entry [] bents))))

(defmacro let
  "Evaluates the exprs in a lexical context in which the symbols in
  the binding-forms are bound to their respective init-exprs or parts
  therein."
  [bindings & body]
  (assert-args let
     (vector? bindings) "a vector for its binding"
     (even? (count bindings)) "an even number of forms in binding vector")
  `(let* ~(destructure bindings) ~@body))

;redefine fn with destructuring and pre/post conditions
(defmacro fn
  "(fn name? [params* ] exprs*)
  (fn name? ([params* ] exprs*)+)

  params => positional-params* , or positional-params* & next-param
  positional-param => binding-form
  next-param => binding-form
  name => symbol

  Defines a function"
  [& sigs]
    (let [name (if (symbol? (first sigs)) (first sigs) nil)
          sigs (if name (next sigs) sigs)
          sigs (if (vector? (first sigs)) (list sigs) sigs)
          psig (fn* [sig]
                 (let [[params & body] sig
                       conds (when (and (next body) (map? (first body))) 
                                           (first body))
                       body (if conds (next body) body)
                       conds (or conds (meta params))
                       pre (:pre conds)
                       post (:post conds)                       
                       body (if post
                              `((let [~'% ~(if (< 1 (count body)) 
                                            `(do ~@body) 
                                            (first body))]
                                 ~@(map (fn* [c] `(assert ~c)) post)
                                 ~'%))
                              body)
                       body (if pre
                              (concat (map (fn* [c] `(assert ~c)) pre) 
                                      body)
                              body)]
                   (if (every? symbol? params)
                     (cons params body)
                     (loop [params params
                            new-params []
                            lets []]
                       (if params
                         (if (symbol? (first params))
                           (recur (next params) (conj new-params (first params)) lets)
                           (let [gparam (gensym "p__")]
                             (recur (next params) (conj new-params gparam)
                                    (-> lets (conj (first params)) (conj gparam)))))
                         `(~new-params
                           (let ~lets
                             ~@body)))))))
          new-sigs (map psig sigs)]
      (with-meta
        (if name
          (list* 'fn* name new-sigs)
          (cons 'fn* new-sigs))
        (meta &form))))

(defmacro loop
  "Evaluates the exprs in a lexical context in which the symbols in
  the binding-forms are bound to their respective init-exprs or parts
  therein. Acts as a recur target."
  [bindings & body]
    (assert-args loop
      (vector? bindings) "a vector for its binding"
      (even? (count bindings)) "an even number of forms in binding vector")
    (let [db (destructure bindings)]
      (if (= db bindings)
        `(loop* ~bindings ~@body)
        (let [vs (take-nth 2 (drop 1 bindings))
              bs (take-nth 2 bindings)
              gs (map (fn [b] (if (symbol? b) b (gensym))) bs)
              bfs (reduce (fn [ret [b v g]]
                            (if (symbol? b)
                              (conj ret g v)
                              (conj ret g v b g)))
                          [] (map vector bs vs gs))]
          `(let ~bfs
             (loop* ~(vec (interleave gs gs))
               (let ~(vec (interleave bs gs))
                 ~@body)))))))

(defmacro when-first
  "bindings => x xs

  Same as (when (seq xs) (let [x (first xs)] body))"
  [bindings & body]
  (assert-args when-first
     (vector? bindings) "a vector for its binding"
     (= 2 (count bindings)) "exactly 2 forms in binding vector")
  (let [[x xs] bindings]
    `(when (seq ~xs)
       (let [~x (first ~xs)]
         ~@body))))

(defmacro lazy-cat
  "Expands to code which yields a lazy sequence of the concatenation
  of the supplied colls.  Each coll expr is not evaluated until it is
  needed.
  
  (lazy-cat xs ys zs) === (concat (lazy-seq xs) (lazy-seq ys) (lazy-seq zs))" 
  [& colls]
  `(concat ~@(map #(list `lazy-seq %) colls)))

(defmacro for
  "List comprehension. Takes a vector of one or more
   binding-form/collection-expr pairs, each followed by zero or more
   modifiers, and yields a lazy sequence of evaluations of expr.
   Collections are iterated in a nested fashion, rightmost fastest,
   and nested coll-exprs can refer to bindings created in prior
   binding-forms.  Supported modifiers are: :let [binding-form expr ...],
   :while test, :when test.

  (take 100 (for [x (range 100000000) y (range 1000000) :while (< y x)] [x y]))"
  [seq-exprs body-expr]
  (assert-args for
    (vector? seq-exprs) "a vector for its binding"
    (even? (count seq-exprs)) "an even number of forms in binding vector")
  (let [to-groups (fn [seq-exprs]
    (reduce (fn [groups [k v]]
      (if (keyword? k)
        (conj (pop groups) (conj (peek groups) [k v]))
        (conj groups [k v])))
      [] (partition 2 seq-exprs)))
       err (fn [& msg] (throw (ArgumentException. #^String (apply str msg))))  ;;; IllegalArgumentException
       emit-bind (fn emit-bind [[[bind expr & mod-pairs]
                                & [[_ next-expr] :as next-groups]]]
      (let [giter (gensym "iter__")
           gxs (gensym "s__")
           do-mod (fn do-mod [[[k v :as pair] & etc]]
          (cond
            (= k :let) `(let ~v ~(do-mod etc))
            (= k :while) `(when ~v ~(do-mod etc))
            (= k :when) `(if ~v
            ~(do-mod etc)
            (recur (rest ~gxs)))
            (keyword? k) (err "Invalid 'for' keyword " k)
            next-groups
            `(let [iterys# ~(emit-bind next-groups)
                  fs# (seq (iterys# ~next-expr))]
              (if fs#
                (concat fs# (~giter (rest ~gxs)))
                (recur (rest ~gxs))))
            :else `(cons ~body-expr
                         (~giter (rest ~gxs)))))]
          (if next-groups
                        #_"not the inner-most loop"
                        `(fn ~giter [~gxs]
                           (lazy-seq
                             (loop [~gxs ~gxs]
                               (when-first [~bind ~gxs]
                                 ~(do-mod mod-pairs)))))
                        #_"inner-most loop"
                        (let [gi (gensym "i__")
                              gb (gensym "b__")
                              do-cmod (fn do-cmod [[[k v :as pair] & etc]]
                                        (cond
                                          (= k :let) `(let ~v ~(do-cmod etc))
                                          (= k :while) `(when ~v ~(do-cmod etc))
                                          (= k :when) `(if ~v
                                                         ~(do-cmod etc)
                                                         (recur
                                                           (unchecked-inc ~gi)))
                                          (keyword? k)
                                            (err "Invalid 'for' keyword " k)
                                          :else
                                            `(do (chunk-append ~gb ~body-expr)
                                                 (recur (unchecked-inc ~gi)))))]
                          `(fn ~giter [~gxs]
                             (lazy-seq
                               (loop [~gxs ~gxs]
                                 (when-let [~gxs (seq ~gxs)]
                                   (if (chunked-seq? ~gxs)
                                     (let [c# (chunk-first ~gxs)
                                           size# (int (count c#))
                                           ~gb (chunk-buffer size#)]
                                       (if (loop [~gi (int 0)]
                                             (if (< ~gi size#)
                                               (let [~bind (.nth c# ~gi)]
                                                 ~(do-cmod mod-pairs))
                                               true))
                                         (chunk-cons
                                           (chunk ~gb)
                                           (~giter (chunk-rest ~gxs)))
                                         (chunk-cons (chunk ~gb) nil)))
                                     (let [~bind (first ~gxs)]
                                       ~(do-mod mod-pairs)))))))))))]
    `(let [iter# ~(emit-bind (to-groups seq-exprs))]
      (iter# ~(second seq-exprs)))))

(defmacro comment
  "Ignores body, yields nil"
  [& body])

(defmacro with-out-str
  "Evaluates exprs in a context in which *out* is bound to a fresh
  StringWriter.  Returns the string created by any nested printing
  calls."
  [& body]
  `(let [s# (new System.IO.StringWriter)]   ;;; Was java.io.StringWriter
     (binding [*out* s#]
       ~@body
       (str s#))))

(defmacro with-in-str
  "Evaluates body in a context in which *in* is bound to a fresh
  StringReader initialized with the string s."
  [s & body]
  `(with-open [s# (-> (System.IO.StringReader. ~s) clojure.lang.LineNumberingTextReader.)]  ;;; were java.io.StringReader & clojure.lang.LineNumberingPushbackReader
     (binding [*in* s#]
       ~@body)))

(defn pr-str
  "pr to a string, returning it"
  {:tag String}
  [& xs]
    (with-out-str
     (apply pr xs)))

(defn prn-str
  "prn to a string, returning it"
  {:tag String}
  [& xs]
  (with-out-str
   (apply prn xs)))

(defn print-str
  "print to a string, returning it"
  {:tag String}
  [& xs]
    (with-out-str
     (apply print xs)))

(defn println-str
  "println to a string, returning it"
  {:tag String}
  [& xs]
    (with-out-str
     (apply println xs)))

(defmacro assert
  "Evaluates expr and throws an exception if it does not evaluate to
 logical true."
  [x]
  (when *assert*
    `(when-not ~x
       (throw (new Exception (str "Assert failed: " (pr-str '~x)))))))

(defn test
  "test [v] finds fn at key :test in var metadata and calls it,
  presuming failure will throw exception"
  [v]
    (let [f (:test (meta v))]
      (if f
        (do (f) :ok)
        :no-test)))
;;; Had to add a bogus class clojure.lang.JReMatcher to make the re-* functions work.
(defn re-pattern
  "Returns an instance of java.util.regex.Pattern, for use, e.g. in
  re-matcher."
  {:tag System.Text.RegularExpressions.Regex }                           ;;; {:tag java.util.regex.Pattern}
  [s] (if (instance? System.Text.RegularExpressions.Regex s)             ;;; java.util.regex.Pattern
        s
        (System.Text.RegularExpressions.Regex. s)))                      ;;; (. java.util.regex.Pattern (compile s))))

(defn re-matcher
  "Returns an instance of java.util.regex.Matcher, for use, e.g. in
  re-find."
  {:tag clojure.lang.JReMatcher}                                         ;;; {:tag java.util.regex.Matcher}
  [#^System.Text.RegularExpressions.Regex re s]                          ;;; java.util.regex.Pattern
    (clojure.lang.JReMatcher. re s))                                     ;;; (. re (matcher s)))

(defn re-groups
  "Returns the groups from the most recent match/find. If there are no
  nested groups, returns a string of the entire match. If there are
  nested groups, returns a vector of the groups, the first element
  being the entire match."
  [#^clojure.lang.JReMatcher m]                                          ;;; java.util.regex.Matcher
    (let [gc (. m (groupCount))]
      (if (zero? gc)
        (. m (group))
        (loop [ret [] c 0]
          (if (<= c gc)
            (recur (conj ret (. m (group c))) (inc c))
            ret)))))

(defn re-seq
  "Returns a lazy sequence of successive matches of pattern in string,
  using java.util.regex.Matcher.find(), each such match processed with
  re-groups."
  [#^System.Text.RegularExpressions.Regex re s]                          ;;; java.util.regex.Pattern
  (let [m (re-matcher re s)]
    ((fn step []
       (when (. m (find))
         (cons (re-groups m) (lazy-seq (step))))))))

(defn re-matches
  "Returns the match, if any, of string to pattern, using
  java.util.regex.Matcher.matches().  Uses re-groups to return the
  groups."
  [#^System.Text.RegularExpressions.Regex re s]                          ;;; java.util.regex.Pattern
    (let [m (re-matcher re s)]
      (when (. m (matches))
        (re-groups m))))


(defn re-find
  "Returns the next regex match, if any, of string to pattern, using
  java.util.regex.Matcher.find().  Uses re-groups to return the
  groups."
  ([#^clojure.lang.JReMatcher m]                                        ;;; java.util.regex.Matcher
   (when (. m (find))
     (re-groups m)))
  ([#^System.Text.RegularExpressions.Regex re s]                        ;;; java.util.regex.Pattern
   (let [m (re-matcher re s)]
     (re-find m))))

(defn rand
  "Returns a random floating point number between 0 (inclusive) and
  n (default 1) (exclusive)."
  ([] (. clojure.lang.RT (random)))  ;;; Math ==> RT.  No Math.random in CLR.
  ([n] (* n (rand))))

(defn rand-int
  "Returns a random integer between 0 (inclusive) and n (exclusive)."
  [n] (int (rand n)))

(defmacro defn-
  "same as defn, yielding non-public def"
  [name & decls]
    (list* `defn (with-meta name (assoc (meta name) :private true)) decls))

(defn print-doc [v]
  (println "-------------------------")
  (println (str (ns-name (:ns (meta v))) "/" (:name (meta v))))
  (prn (:arglists (meta v)))
  (when (:macro (meta v))
    (println "Macro"))
  (println " " (:doc (meta v))))

(defn find-doc
  "Prints documentation for any var whose documentation or name
 contains a match for re-string"
  [re-string]
    (let [re  (re-pattern re-string)]
      (doseq [ns (all-ns)
              v (sort-by (comp :name meta) (vals (ns-interns ns)))
              :when (and (:doc (meta v))
                         (or (re-find (re-matcher re (:doc (meta v))))
                             (re-find (re-matcher re (str (:name (meta v)))))))]
               (print-doc v))))

(defn special-form-anchor
  "Returns the anchor tag on http://clojure.org/special_forms for the
  special form x, or nil"
  [x]
  (#{'. 'def 'do 'fn 'if 'let 'loop 'monitor-enter 'monitor-exit 'new
  'quote 'recur 'set! 'throw 'try 'var} x))

(defn syntax-symbol-anchor
  "Returns the anchor tag on http://clojure.org/special_forms for the
  special form that uses syntax symbol x, or nil"
  [x]
  ({'& 'fn 'catch 'try 'finally 'try} x))

(defn print-special-doc
  [name type anchor]
  (println "-------------------------")
  (println name)
  (println type)
  (println (str "  Please see http://clojure.org/special_forms#" anchor)))
;;; None of the doc stuff tested yet -- need printing and RE.
(defn print-namespace-doc
  "Print the documentation string of a Namespace."
  [nspace]
  (println "-------------------------")
  (println (str (ns-name nspace)))
  (println " " (:doc (meta nspace))))

(defmacro doc
  "Prints documentation for a var or special form given its name"
  [name]
  (cond
   (special-form-anchor `~name)
   `(print-special-doc '~name "Special Form" (special-form-anchor '~name))
   (syntax-symbol-anchor `~name)
   `(print-special-doc '~name "Syntax Symbol" (syntax-symbol-anchor '~name))
   :else
    (let [nspace (find-ns name)]
      (if nspace
        `(print-namespace-doc ~nspace)
        `(print-doc (var ~name))))))
 
 (defn tree-seq
  "Returns a lazy sequence of the nodes in a tree, via a depth-first walk.
   branch? must be a fn of one arg that returns true if passed a node
   that can have children (but may not).  children must be a fn of one
   arg that returns a sequence of the children. Will only be called on
   nodes for which branch? returns true. Root is the root node of the
  tree."  
   [branch? children root]
   (let [walk (fn walk [node]
                (lazy-seq
                 (cons node
                  (when (branch? node)
                    (mapcat walk (children node))))))]
     (walk root)))

(defn file-seq
  "A tree seq on java.io.Files"
  [dir]
    (tree-seq
       (fn [x] (instance? System.IO.DirectoryInfo x))                       ;;; (fn [#^java.io.File f] (. f (isDirectory)))
       (fn [#^System.IO.DirectoryInfo d] (seq (.GetFileSystemInfos d)))     ;;; (fn [#^java.io.File d] (seq (. d (listFiles))))
     dir))

(defn xml-seq
  "A tree seq on the xml elements as per xml/parse"
  [root]
    (tree-seq
     (complement string?)
     (comp seq :content)
     root))

(defn special-symbol?
  "Returns true if s names a special form"
  [s]
    (contains? (. clojure.lang.Compiler _specials) s))   ;;; specials => _specials, because I'm stubborn

(defn var?
  "Returns true if v is of type clojure.lang.Var"
  [v] (instance? clojure.lang.Var v))

(defn slurp
  "Reads the file named by f using the encoding enc into a string
  and returns it."
  ([f] (slurp f (. System.Text.Encoding Default)))      ;;; (slurp f (.name (java.nio.charset.Charset/defaultCharset))))
  ([#^String f  #^System.Text.Encoding enc]             ;;; [#^String f #^String enc]
  (with-open [ r ( new System.IO.StreamReader f enc)]   ;;; (new java.io.BufferedReader
                                                        ;;;   (new java.io.InputStreamReader
                                                        ;;;      (new java.io.FileInputStream f) enc))]
    (let [sb (new StringBuilder)]
      (loop [c (.Read r)]                      ;;; read -> Read
        (if (neg? c)
          (str sb)
          (do
            (.Append sb (char c))              ;;; append -> Append
            (recur (.Read r)))))))))           ;;; read -> Read

(defn subs
  "Returns the substring of s beginning at start inclusive, and ending
  at end (defaults to length of string), exclusive."
  ([#^String s start] (. s (Substring start)))             ;; substring => Substring
  ([#^String s start end] (. s (Substring start (- end start)))))    ;; was (substring start end) -- different interpretation of second arg

(defn max-key
  "Returns the x for which (k x), a number, is greatest."
  ([k x] x)
  ([k x y] (if (> (k x) (k y)) x y))
  ([k x y & more]
   (reduce #(max-key k %1 %2) (max-key k x y) more)))

(defn min-key
  "Returns the x for which (k x), a number, is least."
  ([k x] x)
  ([k x y] (if (< (k x) (k y)) x y))
  ([k x y & more]
   (reduce #(min-key k %1 %2) (min-key k x y) more)))

(defn distinct
  "Returns a lazy sequence of the elements of coll with duplicates removed"
  [coll]
    (let [step (fn step [xs seen]
                   (lazy-seq
                    ((fn [[f :as xs] seen]
                      (when-let [s (seq xs)]
                        (if (contains? seen f) 
                          (recur (rest s) seen)
                          (cons f (step (rest s) (conj seen f))))))
                     xs seen)))]
      (step coll #{})))



(defn replace
  "Given a map of replacement pairs and a vector/collection, returns a
  vector/seq with any elements = a key in smap replaced with the
  corresponding val in smap"
  [smap coll]
    (if (vector? coll)
      (reduce (fn [v i]
                (if-let [e (find smap (nth v i))]
                        (assoc v i (val e))
                        v))
              coll (range (count coll)))
      (map #(if-let [e (find smap %)] (val e) %) coll)))

(defmacro dosync
  "Runs the exprs (in an implicit do) in a transaction that encompasses
  exprs and any nested calls.  Starts a transaction if none is already
  running on this thread. Any uncaught exception will abort the
  transaction and flow out of dosync. The exprs may be run more than
  once, but any effects on Refs will be atomic."
  [& exprs]
  `(sync nil ~@exprs))

(defmacro with-precision
  "Sets the precision and rounding mode to be used for BigDecimal operations.

  Usage: (with-precision 10 (/ 1M 3))
  or:    (with-precision 10 :rounding HALF_DOWN (/ 1M 3))

  The rounding mode is one of CEILING, FLOOR, HALF_UP, HALF_DOWN,
  HALF_EVEN, UP, DOWN and UNNECESSARY; it defaults to HALF_UP."
  [precision & exprs]
    (let [[body rm] (if (= (first exprs) :rounding)
                      [(next (next exprs))
                       `((Enum/Parse clojure.lang.BigDecimal+RoundingMode (name '~(second exprs))))]       ;;; `((. java.math.RoundingMode ~(second exprs)))]
                      [exprs nil])]
      `(binding [*math-context* (clojure.lang.BigDecimal+Context.  ~precision ~@rm)]              ;;; (java.math.MathContext. ~precision ~@rm)]
         ~@body))) 

(defn mk-bound-fn
  {:private true}
  [#^clojure.lang.Sorted sc test key]
  (fn [e]
    (test (.. sc comparator (compare (. sc entryKey e) key)) 0)))

(defn subseq
  "sc must be a sorted collection, test(s) one of <, <=, > or
  >=. Returns a seq of those entries with keys ek for
  which (test (.. sc comparator (compare ek key)) 0) is true"
  ([#^clojure.lang.Sorted sc test key]
   (let [include (mk-bound-fn sc test key)]
     (if (#{> >=} test)
       (when-let [[e :as s] (. sc seqFrom key true)]
         (if (include e) s (next s)))
       (take-while include (. sc seq true)))))
  ([#^clojure.lang.Sorted sc start-test start-key end-test end-key]
   (when-let [[e :as s] (. sc seqFrom start-key true)]
     (take-while (mk-bound-fn sc end-test end-key)
                 (if ((mk-bound-fn sc start-test start-key) e) s (next s))))))

(defn rsubseq
  "sc must be a sorted collection, test(s) one of <, <=, > or
  >=. Returns a reverse seq of those entries with keys ek for
  which (test (.. sc comparator (compare ek key)) 0) is true"
  ([#^clojure.lang.Sorted sc test key]
   (let [include (mk-bound-fn sc test key)]
     (if (#{< <=} test)
       (when-let [[e :as s] (. sc seqFrom key false)]
         (if (include e) s (next s)))
       (take-while include (. sc seq false)))))
  ([#^clojure.lang.Sorted sc start-test start-key end-test end-key]
   (when-let [[e :as s] (. sc seqFrom end-key false)]
     (take-while (mk-bound-fn sc start-test start-key)
                 (if ((mk-bound-fn sc end-test end-key) e) s (next s))))))

(defn repeatedly
  "Takes a function of no args, presumably with side effects, and
  returns an infinite (or length n if supplied) lazy sequence of calls
  to it" 
  ([f] (lazy-seq (cons (f) (repeatedly f))))
  ([n f] (take n (repeatedly f))))
;;; What is CLR equivalent -- should this just be a no-op?
;(defn add-classpath
;  "DEPRECATED 
;
;  Adds the url (String or URL object) to the classpath per
;  URLClassLoader.addURL"
;  [url]
;  (println "WARNING: add-classpath is deprecated")
;  (clojure.lang.RT/addURL url))



(defn hash
  "Returns the hash code of its argument"
  [x] (. clojure.lang.Util (hash x)))

(defn interpose
  "Returns a lazy seq of the elements of coll separated by sep"
  [sep coll] (drop 1 (interleave (repeat sep) coll)))

(defmacro definline
  "Experimental - like defmacro, except defines a named function whose
  body is the expansion, calls to which may be expanded inline as if
  it were a macro. Cannot be used with variadic (&) args."
  [name & decl]
  (let [[pre-args [args expr]] (split-with (comp not vector?) decl)]
    `(do
       (defn ~name ~@pre-args ~args ~(apply (eval (list `fn args expr)) args))
       (alter-meta! (var ~name) assoc :inline (fn ~name ~args ~expr))
       (var ~name))))
       
(defn empty
  "Returns an empty collection of the same category as coll, or nil"
  [coll]
  (when (instance? clojure.lang.IPersistentCollection coll)
    (.empty #^clojure.lang.IPersistentCollection coll)))

(defmacro amap
  "Maps an expression across an array a, using an index named idx, and
  return value named ret, initialized to a clone of a, then setting 
  each element of ret to the evaluation of expr, returning the new 
  array ret."
  [a idx ret expr]
  `(let [a# ~a
         ~ret (aclone a#)]
     (loop  [~idx (int 0)]
       (if (< ~idx  (alength a#))
         (do
           (aset ~ret ~idx ~expr)
           (recur (unchecked-inc ~idx)))
         ~ret))))

(defmacro areduce
  "Reduces an expression across an array a, using an index named idx,
  and return value named ret, initialized to init, setting ret to the 
  evaluation of expr at each step, returning ret."
  [a idx ret init expr]
  `(let [a# ~a]
     (loop  [~idx (int 0) ~ret ~init]
       (if (< ~idx  (alength a#))
         (recur (unchecked-inc ~idx) ~expr)
         ~ret))))

(defn float-array
  "Creates an array of floats"
  {:inline (fn [& args] `(. clojure.lang.Numbers float_array ~@args))
   :inline-arities #{1 2}}
  ([size-or-seq] (. clojure.lang.Numbers float_array size-or-seq))
  ([size init-val-or-seq] (. clojure.lang.Numbers float_array size init-val-or-seq)))

(defn boolean-array
  "Creates an array of booleans"
  {:inline (fn [& args] `(. clojure.lang.Numbers boolean_array ~@args))
   :inline-arities #{1 2}}
  ([size-or-seq] (. clojure.lang.Numbers boolean_array size-or-seq))
  ([size init-val-or-seq] (. clojure.lang.Numbers boolean_array size init-val-or-seq)))

(defn byte-array
  "Creates an array of bytes"
  {:inline (fn [& args] `(. clojure.lang.Numbers byte_array ~@args))
   :inline-arities #{1 2}}
  ([size-or-seq] (. clojure.lang.Numbers byte_array size-or-seq))
  ([size init-val-or-seq] (. clojure.lang.Numbers byte_array size init-val-or-seq)))

(defn char-array
  "Creates an array of chars"
  {:inline (fn [& args] `(. clojure.lang.Numbers char_array ~@args))
   :inline-arities #{1 2}}
  ([size-or-seq] (. clojure.lang.Numbers char_array size-or-seq))
  ([size init-val-or-seq] (. clojure.lang.Numbers char_array size init-val-or-seq)))

(defn short-array
  "Creates an array of shorts"
  {:inline (fn [& args] `(. clojure.lang.Numbers short_array ~@args))
   :inline-arities #{1 2}}
  ([size-or-seq] (. clojure.lang.Numbers short_array size-or-seq))
  ([size init-val-or-seq] (. clojure.lang.Numbers short_array size init-val-or-seq)))

(defn double-array
  "Creates an array of doubles"
  {:inline (fn [& args] `(. clojure.lang.Numbers double_array ~@args))
   :inline-arities #{1 2}}
  ([size-or-seq] (. clojure.lang.Numbers double_array size-or-seq))
  ([size init-val-or-seq] (. clojure.lang.Numbers double_array size init-val-or-seq)))

(defn object-array
  "Creates an array of objects"
  {:inline (fn [arg] `(. clojure.lang.RT object_array ~arg))
   :inline-arities #{1}}
  ([size-or-seq] (. clojure.lang.RT object_array size-or-seq)))

(defn int-array
  "Creates an array of ints"
  {:inline (fn [& args] `(. clojure.lang.Numbers int_array ~@args))
   :inline-arities #{1 2}}
  ([size-or-seq] (. clojure.lang.Numbers int_array size-or-seq))
  ([size init-val-or-seq] (. clojure.lang.Numbers int_array size init-val-or-seq)))

(defn long-array
  "Creates an array of longs"
  {:inline (fn [& args] `(. clojure.lang.Numbers long_array ~@args))
   :inline-arities #{1 2}}
  ([size-or-seq] (. clojure.lang.Numbers long_array size-or-seq))
  ([size init-val-or-seq] (. clojure.lang.Numbers long_array size init-val-or-seq)))

(definline booleans
  "Casts to boolean[]"
  [xs] `(. clojure.lang.Numbers booleans ~xs))

(definline bytes
  "Casts to bytes[]"
  [xs] `(. clojure.lang.Numbers bytes ~xs))

(definline chars
  "Casts to chars[]"
  [xs] `(. clojure.lang.Numbers chars ~xs))

(definline shorts
  "Casts to shorts[]"
  [xs] `(. clojure.lang.Numbers shorts ~xs))

(definline floats
  "Casts to float[]"
  [xs] `(. clojure.lang.Numbers floats ~xs))

(definline ints
  "Casts to int[]"
  [xs] `(. clojure.lang.Numbers ints ~xs))

(definline doubles
  "Casts to double[]"
  [xs] `(. clojure.lang.Numbers doubles ~xs))

(definline longs
  "Casts to long[]"
  [xs] `(. clojure.lang.Numbers longs ~xs))

;(import '(java.util.concurrent BlockingQueue LinkedBlockingQueue))
;;;NOT WORTH THE EFFORT AT THE MOMENT
;(defn seque
;  "Creates a queued seq on another (presumably lazy) seq s. The queued
;  seq will produce a concrete seq in the background, and can get up to
;  n items ahead of the consumer. n-or-q can be an integer n buffer
;  size, or an instance of java.util.concurrent BlockingQueue. Note
;  that reading from a seque can block if the reader gets ahead of the
;  producer."
;  ([s] (seque 100 s))
;  ([n-or-q s]
;   (let [#^BlockingQueue q (if (instance? BlockingQueue n-or-q)
;                             n-or-q
;                             (LinkedBlockingQueue. (int n-or-q)))
;         NIL (Object.) ;nil sentinel since LBQ doesn't support nils
;         agt (agent (seq s))
;         fill (fn [s]
;                (try
;                  (loop [[x & xs :as s] s]
;                    (if s
;                      (if (.offer q (if (nil? x) NIL x))
;                        (recur xs)
;                        s)
;                      (.put q q))) ; q itself is eos sentinel
;                  (catch Exception e
;                    (.put q q)
;                    (throw e))))
;         drain (fn drain []
;                 (lazy-seq
;                  (let [x (.take q)]
;                    (if (identical? x q) ;q itself is eos sentinel
;                      (do @agt nil)  ;touch agent just to propagate errors
;                      (do
;                        (send-off agt fill)
;                        (cons (if (identical? x NIL) nil x) (drain)))))))]
;     (send-off agt fill)
;     (drain))))

(defn class?
  "Returns true if x is an instance of Class"
  [x] (instance? Type x))               ;;  Class ==> Type

(defn alter-var-root
  "Atomically alters the root binding of var v by applying f to its
  current value plus any args"
  [#^clojure.lang.Var v f & args] (.alterRoot v f args))

(defn bound?
  "Returns true if all of the vars provided as arguments have any bound value, root or thread-local.
   Implies that deref'ing the provided vars will succeed. Returns true if no vars are provided."
  [& vars]
  (every? #(.isBound #^clojure.lang.Var %) vars))

(defn thread-bound?
  "Returns true if all of the vars provided as arguments have thread-local bindings.
   Implies that set!'ing the provided vars will succeed.  Returns true if no vars are provided."
  [& vars]
  (every? #(.getThreadBinding #^clojure.lang.Var %) vars))

(defn make-hierarchy
  "Creates a hierarchy object for use with derive, isa? etc."
  [] {:parents {} :descendants {} :ancestors {}})

(def #^{:private true}
     global-hierarchy (make-hierarchy))

(defn not-empty
  "If coll is empty, returns nil, else coll"
  [coll] (when (seq coll) coll))

(defn bases
  "Returns the immediate superclass and direct interfaces of c, if any"
  [#^Type c]                             ;;;  Class ==> Type
  (when c
    (let [i (.GetInterfaces c)             ;;;  .getInterfaces ==> .GetInterfaces
          s (.BaseType c)]                 ;;;  .getSuperclass ==> BaseType
      (not-empty
        (if s (cons s i) i)))))

(defn supers
  "Returns the immediate and indirect superclasses and interfaces of c, if any"
  [#^Type class]                                ;;;  Class ==> Type
  (loop [ret (set (bases class)) cs ret]
    (if (seq cs)
      (let [c (first cs) bs (bases c)]
        (recur (into ret bs) (into (disj cs c) bs)))
      (not-empty ret))))

(defn isa?
  "Returns true if (= child parent), or child is directly or indirectly derived from
  parent, either via a Java type inheritance relationship or a
  relationship established via derive. h must be a hierarchy obtained
  from make-hierarchy, if not supplied defaults to the global
  hierarchy"
  ([child parent] (isa? global-hierarchy child parent))
  ([h child parent]
   (or (= child parent)
       (and (class? parent) (class? child)
            (. #^Type parent IsAssignableFrom child))  ;;; Class ==> Type, isAssignableFrom 
       (contains? ((:ancestors h) child) parent)
       (and (class? child) (some #(contains? ((:ancestors h) %) parent) (supers child)))
       (and (vector? parent) (vector? child)
            (= (count parent) (count child))
            (loop [ret true i 0]
              (if (or (not ret) (= i (count parent)))
                ret
                (recur (isa? h (child i) (parent i)) (inc i))))))))

(defn parents
  "Returns the immediate parents of tag, either via a Java type
  inheritance relationship or a relationship established via derive. h
  must be a hierarchy obtained from make-hierarchy, if not supplied
  defaults to the global hierarchy"
  ([tag] (parents global-hierarchy tag))
  ([h tag] (not-empty
            (let [tp (get (:parents h) tag)]
              (if (class? tag)
                (into (set (bases tag)) tp)
                tp)))))
;;; NOT TESTED YET
(defn ancestors
  "Returns the immediate and indirect parents of tag, either via a Java type
  inheritance relationship or a relationship established via derive. h
  must be a hierarchy obtained from make-hierarchy, if not supplied
  defaults to the global hierarchy"
  ([tag] (ancestors global-hierarchy tag))
  ([h tag] (not-empty
            (let [ta (get (:ancestors h) tag)]
              (if (class? tag)
                (let [superclasses (set (supers tag))]
                  (reduce into superclasses
                    (cons ta
                          (map #(get (:ancestors h) %) superclasses))))
                ta)))))
;;; NOT TESTED YET
(defn descendants
  "Returns the immediate and indirect children of tag, through a
  relationship established via derive. h must be a hierarchy obtained
  from make-hierarchy, if not supplied defaults to the global
  hierarchy. Note: does not work on Java type inheritance
  relationships."
  ([tag] (descendants global-hierarchy tag))
  ([h tag] (if (class? tag)
             (throw (NotImplementedException. "Can't get descendants of classes"))    ;;; java.lang.UnsupportedOperationException --> NotImplementedException
             (not-empty (get (:descendants h) tag)))))
;;; NOT TESTED YET
(defn derive
  "Establishes a parent/child relationship between parent and
  tag. Parent must be a namespace-qualified symbol or keyword and
  child can be either a namespace-qualified symbol or keyword or a
  class. h must be a hierarchy obtained from make-hierarchy, if not
  supplied defaults to, and modifies, the global hierarchy."
  ([tag parent]
   (assert (namespace parent))
   (assert (or (class? tag) (and (instance? clojure.lang.Named tag) (namespace tag))))

   (alter-var-root #'global-hierarchy derive tag parent) nil)
  ([h tag parent]
   (assert (not= tag parent))
   (assert (or (class? tag) (instance? clojure.lang.Named tag)))
   (assert (instance? clojure.lang.Named parent))

   (let [tp (:parents h)
         td (:descendants h)
         ta (:ancestors h)
         tf (fn [m source sources target targets]
              (reduce (fn [ret k]
                        (assoc ret k
                               (reduce conj (get targets k #{}) (cons target (targets target)))))
                      m (cons source (sources source))))]
     (or
      (when-not (contains? (tp tag) parent)
        (when (contains? (ta tag) parent)
          (throw (Exception. (print-str tag "already has" parent "as ancestor"))))
        (when (contains? (ta parent) tag)
          (throw (Exception. (print-str "Cyclic derivation:" parent "has" tag "as ancestor"))))
        {:parents (assoc (:parents h) tag (conj (get tp tag #{}) parent))
         :ancestors (tf (:ancestors h) tag td parent ta)
         :descendants (tf (:descendants h) parent ta tag td)})
      h))))
;;; NOT TESTED YET
(defn underive
  "Removes a parent/child relationship between parent and
  tag. h must be a hierarchy obtained from make-hierarchy, if not
  supplied defaults to, and modifies, the global hierarchy."
  ([tag parent] (alter-var-root #'global-hierarchy underive tag parent) nil)
  ([h tag parent]
   (let [tp (:parents h)
         td (:descendants h)
         ta (:ancestors h)
         tf (fn [m source sources target targets]
              (reduce
               (fn [ret k]
                 (assoc ret k
                        (reduce disj (get targets k) (cons target (targets target)))))
               m (cons source (sources source))))]
     (if (contains? (tp tag) parent)
       {:parent (assoc (:parents h) tag (disj (get tp tag) parent))
        :ancestors (tf (:ancestors h) tag td parent ta)
        :descendants (tf (:descendants h) parent ta tag td)}
       h))))


(defn distinct?
  "Returns true if no two of the arguments are ="
  {:tag Boolean}
  ([x] true)
  ([x y] (not (= x y)))
  ([x y & more]
   (if (not= x y)
     (loop [s #{x y} [x & etc :as xs] more]
       (if xs
         (if (contains? s x)
           false
           (recur (conj s x) etc))
         true))
     false)))
;;; Not clear what to work against here.
;(defn resultset-seq
;  "Creates and returns a lazy sequence of structmaps corresponding to
;  the rows in the java.sql.ResultSet rs"
;  [#^java.sql.ResultSet rs]
;    (let [rsmeta (. rs (getMetaData))
;          idxs (range 1 (inc (. rsmeta (getColumnCount))))
;          keys (map (comp keyword #(.toLowerCase #^String %))
;                    (map (fn [i] (. rsmeta (getColumnLabel i))) idxs))
;          check-keys
;                (or (apply distinct? keys)
;                    (throw (Exception. "ResultSet must have unique column labels")))
;          row-struct (apply create-struct keys)
;          row-values (fn [] (map (fn [#^Integer i] (. rs (getObject i))) idxs))
;          rows (fn thisfn []
;                 (when (. rs (next))
;                   (cons (apply struct row-struct (row-values)) (lazy-seq (thisfn)))))]
;      (rows)))

(defn iterator-seq
  "Returns a seq on a java.util.Iterator. Note that most collections
  providing iterators implement Iterable and thus support seq directly."
  [iter]
  (clojure.lang.EnumeratorSeq/create iter))   ;;; IteratorSeq

(defn enumeration-seq
  "Returns a seq on a java.util.Enumeration"
  [e]
  (clojure.lang.EnumeratorSeq/create e))     ;;; EnumerationSeq
;;; Should we make compatible with Java?
(defn format
  "Formats a string using java.lang.String.format, see java.util.Formatter for format
  string syntax"
  {:tag String}
  [fmt & args]
  (clojure.lang.Printf/Format fmt (to-array args)))        ;;;(String/format fmt (to-array args)))

(defn printf
  "Prints formatted output, as per format"
  [fmt & args]
  (print (apply format fmt args)))

(declare gen-class)
;;; no clear equivalent for us
(defmacro with-loading-context [& body]
  `((fn loading# [] 
        (. clojure.lang.Var (pushThreadBindings  {}))   ;;;{clojure.lang.Compiler/LOADER  
                                                        ;;;(.getClassLoader (.getClass #^Object loading#))}))
        (try
         ~@body
         (finally
          (. clojure.lang.Var (popThreadBindings)))))))

(defmacro ns
  "Sets *ns* to the namespace named by name (unevaluated), creating it
  if needed.  references can be zero or more of: (:refer-clojure ...)
  (:require ...) (:use ...) (:import ...) (:load ...) (:gen-class)
  with the syntax of refer-clojure/require/use/import/load/gen-class
  respectively, except the arguments are unevaluated and need not be
  quoted. (:gen-class ...), when supplied, defaults to :name
  corresponding to the ns name, :main true, :impl-ns same as ns, and
  :init-impl-ns true. All options of gen-class are
  supported. The :gen-class directive is ignored when not
  compiling. If :gen-class is not supplied, when compiled only an
  nsname__init.class will be generated. If :refer-clojure is not used, a
  default (refer 'clojure) is used.  Use of ns is preferred to
  individual calls to in-ns/require/use/import:

  (ns foo.bar
    (:refer-clojure :exclude [ancestors printf])
    (:require (clojure.contrib sql sql.tests))
    (:use (my.lib this that))
    (:import (java.util Date Timer Random)
              (java.sql Connection Statement)))"
  {:arglists '([name docstring? attr-map? references*])}
  [name & references]
  (let [process-reference
        (fn [[kname & args]]
          `(~(symbol "clojure.core" (clojure.core/name kname))
             ~@(map #(list 'quote %) args)))
        docstring  (when (string? (first references)) (first references))
        references (if docstring (next references) references)
        name (if docstring
               (vary-meta name assoc :doc docstring)
               name)
        metadata   (when (map? (first references)) (first references))
        references (if metadata (next references) references)
        name (if metadata
               (vary-meta name merge metadata)
               name)
        gen-class-clause (first (filter #(= :gen-class (first %)) references))
        gen-class-call
          (when gen-class-clause
            (list* `gen-class :name (.Replace (str name) \- \_) :impl-ns name :main true (next gen-class-clause)))   ;;; .replace
        references (remove #(= :gen-class (first %)) references)
        ;ns-effect (clojure.core/in-ns name)
        ]
    `(do
       (clojure.core/in-ns '~name)
      
       (with-loading-context

        ~@(when gen-class-call (list gen-class-call))
        ~@(when (and (not= name 'clojure.core) (not-any? #(= :refer-clojure (first %)) references))
            `((clojure.core/refer '~'clojure.core)))
        ~@(map process-reference references)))))

(defmacro refer-clojure
  "Same as (refer 'clojure.core <filters>)"
  [& filters]
  `(clojure.core/refer '~'clojure.core ~@filters))
 
(defmacro defonce
  "defs name to have the root value of the expr iff the named var has no root value,
  else expr is unevaluated"
  [name expr]
  `(let [v# (def ~name)]
     (when-not (.hasRoot v#)
       (def ~name ~expr))))

;;;;;;;;;;; require/use/load, contributed by Stephen C. Gilardi ;;;;;;;;;;;;;;;;;;

(defonce
  #^{:private true
     :doc "A ref to a sorted set of symbols representing loaded libs"}
  *loaded-libs* (ref (sorted-set)))

(defonce
  #^{:private true
     :doc "the set of paths currently being loaded by this thread"}
  *pending-paths* #{})

(defonce
  #^{:private true :doc
     "True while a verbose load is pending"}
  *loading-verbosely* false)

(defn- throw-if
  "Throws an exception with a message if pred is true"
  [pred fmt & args]
  (when pred
    (let [ #^String message (apply format fmt args)
          exception (Exception. message)     
          ;; can't set the stacktrace ---- raw-trace (.getStackTrace exception)   
          ;;                          ---- boring? #(not= (.getMethodName #^StackTraceElement %) "doInvoke")
         ];;                          ---- trace (into-array (drop 2 (drop-while boring? raw-trace)))]
      ;;;                           ---- (.setStackTrace exception trace)
      (throw exception))))

(defn- libspec?
  "Returns true if x is a libspec"
  [x]
  (or (symbol? x)
      (and (vector? x)
           (or
            (nil? (second x))
            (keyword? (second x))))))

(defn- prependss
  "Prepends a symbol or a seq to coll"
  [x coll]
  (if (symbol? x)
    (cons x coll)
    (concat x coll)))

(defn- root-resource
  "Returns the root directory path for a lib"
  {:tag String}
  [lib]
  (str \/
       (.. (name lib)
           (Replace \- \_)       ;;; replace
           (Replace \. \/))))    ;;; replace

(defn- root-directory
  "Returns the root resource path for a lib"
  [lib]
  (let [d (root-resource lib)]
    (subs d 0 (.LastIndexOf d "/"))))    ;;; lastIndexOf

(declare load)

(defn- load-one
  "Loads a lib given its name. If need-ns, ensures that the associated
  namespace exists after loading. If require, records the load so any
  duplicate loads can be skipped."
  [lib need-ns require]
  (load (root-resource lib))
  (throw-if (and need-ns (not (find-ns lib)))
            "namespace '%s' not found after loading '%s'"
            lib (root-resource lib))
  (when require
    (dosync
     (commute *loaded-libs* conj lib))))

(defn- load-all
  "Loads a lib given its name and forces a load of any libs it directly or
  indirectly loads. If need-ns, ensures that the associated namespace
  exists after loading. If require, records the load so any duplicate loads
  can be skipped."
  [lib need-ns require]
  (dosync
   (commute *loaded-libs* #(reduce conj %1 %2)
            (binding [*loaded-libs* (ref (sorted-set))]
              (load-one lib need-ns require)
              @*loaded-libs*))))

(defn- load-lib
  "Loads a lib with options"
  [prefix lib & options]
  (throw-if (and prefix (pos? (.IndexOf (name lib) \.)))               ;;; indexOf  & (int \.)
            "lib names inside prefix lists must not contain periods")
  (let [lib (if prefix (symbol (str prefix \. lib)) lib)
        opts (apply hash-map options)
        {:keys [as reload reload-all require use verbose]} opts
        loaded (contains? @*loaded-libs* lib)
        load (cond reload-all
                   load-all
                   (or reload (not require) (not loaded))
                   load-one)
        need-ns (or as use)
        filter-opts (select-keys opts '(:exclude :only :rename))]
    (binding [*loading-verbosely* (or *loading-verbosely* verbose)]
      (if load
        (load lib need-ns require)
        (throw-if (and need-ns (not (find-ns lib)))
                  "namespace '%s' not found" lib))
      (when (and need-ns *loading-verbosely*)
        (printf "(clojure.core/in-ns '%s)\n" (ns-name *ns*)))
      (when as
        (when *loading-verbosely*
          (printf "(clojure.core/alias '%s '%s)\n" as lib))
        (alias as lib))
      (when use
        (when *loading-verbosely*
          (printf "(clojure.core/refer '%s" lib)
          (doseq [opt filter-opts]
            (printf " %s '%s" (key opt) (print-str (val opt))))
          (printf ")\n"))
        (apply refer lib (mapcat seq filter-opts))))))

(defn- load-libs
  "Loads libs, interpreting libspecs, prefix lists, and flags for
  forwarding to load-lib"
  [& args]
  (let [flags (filter keyword? args)
        opts (interleave flags (repeat true))
        args (filter (complement keyword?) args)]
    ; check for unsupported options
    (let [supported #{:as :reload :reload-all :require :use :verbose} 
          unsupported (seq (remove supported flags))]
      (throw-if unsupported
                (apply str "Unsupported option(s) supplied: "
                     (interpose \, unsupported))))
    ; check a load target was specified
    (throw-if (not (seq args)) "Nothing specified to load")
    (doseq [arg args]
      (if (libspec? arg)
        (apply load-lib nil (prependss arg opts))
        (let [[prefix & args] arg]
          (throw-if (nil? prefix) "prefix cannot be nil")
          (doseq [arg args]
            (apply load-lib prefix (prependss arg opts))))))))

;; Public


(defn require
  "Loads libs, skipping any that are already loaded. Each argument is
  either a libspec that identifies a lib, a prefix list that identifies
  multiple libs whose names share a common prefix, or a flag that modifies
  how all the identified libs are loaded. Use :require in the ns macro
  in preference to calling this directly.

  Libs

  A 'lib' is a named set of resources in classpath whose contents define a
  library of Clojure code. Lib names are symbols and each lib is associated
  with a Clojure namespace and a Java package that share its name. A lib's
  name also locates its root directory within classpath using Java's
  package name to classpath-relative path mapping. All resources in a lib
  should be contained in the directory structure under its root directory.
  All definitions a lib makes should be in its associated namespace.

  'require loads a lib by loading its root resource. The root resource path
  is derived from the lib name in the following manner:
  Consider a lib named by the symbol 'x.y.z; it has the root directory
  <classpath>/x/y/, and its root resource is <classpath>/x/y/z.clj. The root
  resource should contain code to create the lib's namespace (usually by using
  the ns macro) and load any additional lib resources. 

  Libspecs

  A libspec is a lib name or a vector containing a lib name followed by
  options expressed as sequential keywords and arguments.

  Recognized options: :as
  :as takes a symbol as its argument and makes that symbol an alias to the
    lib's namespace in the current namespace.

  Prefix Lists

  It's common for Clojure code to depend on several libs whose names have
  the same prefix. When specifying libs, prefix lists can be used to reduce
  repetition. A prefix list contains the shared prefix followed by libspecs
  with the shared prefix removed from the lib names. After removing the
  prefix, the names that remain must not contain any periods.

  Flags

  A flag is a keyword.
  Recognized flags: :reload, :reload-all, :verbose
  :reload forces loading of all the identified libs even if they are
    already loaded
  :reload-all implies :reload and also forces loading of all libs that the
    identified libs directly or indirectly load via require or use
  :verbose triggers printing information about each load, alias, and refer

  Example:

  The following would load the libraries clojure.zip and clojure.set
  abbreviated as 's'.

  (require '(clojure zip [set :as s]))"
  
  [& args]
  (apply load-libs :require args))

(defn use
  "Like 'require, but also refers to each lib's namespace using
  clojure.core/refer. Use :use in the ns macro in preference to calling
  this directly.

  'use accepts additional options in libspecs: :exclude, :only, :rename.
  The arguments and semantics for :exclude, :only, and :rename are the same
  as those documented for clojure.core/refer."
  [& args] (apply load-libs :require :use args))

(defn loaded-libs
  "Returns a sorted set of symbols naming the currently loaded libs"
  [] @*loaded-libs*)

(defn load
  "Loads Clojure code from resources in classpath. A path is interpreted as
  classpath-relative if it begins with a slash or relative to the root
  directory for the current namespace otherwise."
  [& paths]
  (doseq [#^String path paths]
    (let [#^String path (if (.StartsWith path "/")   ;;; startsWith
                          path
                         (str (root-directory (ns-name *ns*)) \/ path))]
      (when *loading-verbosely*
        (printf "(clojure.core/load \"%s\")\n" path)
        (flush))
;      (throw-if (*pending-paths* path)
;                "cannot load '%s' again while it is loading"
;                path)
      (when-not (*pending-paths* path)
        (binding [*pending-paths* (conj *pending-paths* path)]
          (clojure.lang.RT/load  (.Substring path 1)))))))       ;;; .substring

(defn compile
  "Compiles the namespace named by the symbol lib into a set of
  classfiles. The source for the lib must be in a proper
  classpath-relative directory. The output files will go into the
  directory specified by *compile-path*, and that directory too must
  be in the classpath."
  [lib]
  (binding [*compile-files* true]
    (load-one lib true true))
  lib)

;;;;;;;;;;;;; nested associative ops ;;;;;;;;;;;

(defn get-in
  "returns the value in a nested associative structure, where ks is a sequence of keys"
  [m ks]
  (reduce get m ks))

(defn assoc-in
  "Associates a value in a nested associative structure, where ks is a
  sequence of keys and v is the new value and returns a new nested structure.
  If any levels do not exist, hash-maps will be created."
  [m [k & ks] v]
  (if ks
    (assoc m k (assoc-in (get m k) ks v))
    (assoc m k v)))

(defn update-in
  "'Updates' a value in a nested associative structure, where ks is a
  sequence of keys and f is a function that will take the old value
  and any supplied args and return the new value, and returns a new
  nested structure.  If any levels do not exist, hash-maps will be
  created."
  ([m [k & ks] f & args]
   (if ks
     (assoc m k (apply update-in (get m k) ks f args))
     (assoc m k (apply f (get m k) args)))))


(defn empty?
  "Returns true if coll has no items - same as (not (seq coll)).
  Please use the idiom (seq x) rather than (not (empty? x))"
  [coll] (not (seq coll)))

(defn coll?
  "Returns true if x implements IPersistentCollection"
  [x] (instance? clojure.lang.IPersistentCollection x))

(defn list?
  "Returns true if x implements IPersistentList"
  [x] (instance? clojure.lang.IPersistentList x))

(defn set?
  "Returns true if x implements IPersistentSet"
  [x] (instance? clojure.lang.IPersistentSet x))
  
(defn ifn?
  "Returns true if x implements IFn. Note that many data structures
  (e.g. sets and maps) implement IFn"
  [x] (instance? clojure.lang.IFn x))
  
(defn fn?
  "Returns true if x implements Fn, i.e. is an object created via fn."
  [x] (instance? clojure.lang.Fn x))     
  

(defn associative?
 "Returns true if coll implements Associative"
  [coll] (instance? clojure.lang.Associative coll))

(defn sequential?
 "Returns true if coll implements Sequential"
  [coll] (instance? clojure.lang.Sequential coll))

(defn sorted?
 "Returns true if coll implements Sorted"
  [coll] (instance? clojure.lang.Sorted coll))

(defn counted?
 "Returns true if coll implements count in constant time"
  [coll] (instance? clojure.lang.Counted coll))

(defn reversible?
 "Returns true if coll implements Reversible"
  [coll] (instance? clojure.lang.Reversible coll))

(def
 #^{:doc "bound in a repl thread to the most recent value printed"}
 *1)

(def
 #^{:doc "bound in a repl thread to the second most recent value printed"}
 *2)

(def
 #^{:doc "bound in a repl thread to the third most recent value printed"}
 *3)

(def
 #^{:doc "bound in a repl thread to the most recent exception caught by the repl"}
 *e)

(defn trampoline
  "trampoline can be used to convert algorithms requiring mutual
  recursion without stack consumption. Calls f with supplied args, if
  any. If f returns a fn, calls that fn with no arguments, and
  continues to repeat, until the return value is not a fn, then
  returns that non-fn value. Note that if you want to return a fn as a
  final value, you must wrap it in some data structure and unpack it
  after trampoline returns."
  ([f]
     (let [ret (f)]
       (if (fn? ret)
         (recur ret)
         ret)))
  ([f & args]
     (trampoline #(apply f args))))

(defn intern
  "Finds or creates a var named by the symbol name in the namespace
  ns (which can be a symbol or a namespace), setting its root binding
  to val if supplied. The namespace must exist. The var will adopt any
  metadata from the name symbol.  Returns the var."
  ([ns #^clojure.lang.Symbol name] 
     (let [v (clojure.lang.Var/intern (the-ns ns) name)]
       (when (meta name) (.setMeta v (meta name)))
       v))
  ([ns name val] 
     (let [v (clojure.lang.Var/intern (the-ns ns) name val)]
       (when (meta name) (.setMeta v (meta name)))
       v)))

(defmacro while
  "Repeatedly executes body while test expression is true. Presumes
  some side-effect will cause test to become false/nil. Returns nil"
  [test & body]
  `(loop []
     (when ~test
       ~@body
       (recur))))

(defn memoize
  "Returns a memoized version of a referentially transparent function. The
  memoized version of the function keeps a cache of the mapping from arguments
  to results and, when calls with the same arguments are repeated often, has
  higher performance at the expense of higher memory use."
  [f]
  (let [mem (atom {})]
    (fn [& args]
      (if-let [e (find @mem args)]
        (val e)
        (let [ret (apply f args)]
          (swap! mem assoc args ret)
          ret)))))

(defmacro condp
  "Takes a binary predicate, an expression, and a set of clauses.
  Each clause can take the form of either:

  test-expr result-expr

  test-expr :>> result-fn

  Note :>> is an ordinary keyword.

  For each clause, (pred test-expr expr) is evaluated. If it returns
  logical true, the clause is a match. If a binary clause matches, the
  result-expr is returned, if a ternary clause matches, its result-fn,
  which must be a unary function, is called with the result of the
  predicate as its argument, the result of that call being the return
  value of condp. A single default expression can follow the clauses,
  and its value will be returned if no clause matches. If no default
  expression is provided and no clause matches, an
  IllegalArgumentException is thrown."

  [pred expr & clauses]
  (let [gpred (gensym "pred__")
        gexpr (gensym "expr__")
        emit (fn emit [pred expr args]
               (let [[[a b c :as clause] more] 
                       (split-at (if (= :>> (second args)) 3 2) args)
                       n (count clause)]
                 (cond
                  (= 0 n) `(throw (ArgumentException. (str "No matching clause: " ~expr)))  ;;;IllegalArgumentException
                  (= 1 n) a
                  (= 2 n) `(if (~pred ~a ~expr)
                             ~b
                             ~(emit pred expr more))
                  :else `(if-let [p# (~pred ~a ~expr)]
                           (~c p#)
                           ~(emit pred expr more)))))
        gres (gensym "res__")]
    `(let [~gpred ~pred
           ~gexpr ~expr]
       ~(emit gpred gexpr clauses))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; var documentation ;;;;;;;;;;;;;;;;;;;;;;;;;;

(defmacro add-doc {:private true} [name docstring]
  `(alter-meta! (var ~name)  assoc :doc ~docstring))

(add-doc *file*
  "The path of the file being evaluated, as a String.

  Evaluates to nil when there is no file, eg. in the REPL.")

(add-doc *command-line-args*
  "A sequence of the supplied command line arguments, or nil if
  none were supplied")

(add-doc *warn-on-reflection*
  "When set to true, the compiler will emit warnings when reflection is
  needed to resolve Java method calls or field accesses.

  Defaults to false.")

(add-doc *compile-path*
  "Specifies the directory where 'compile' will write out .class
  files. This directory must be in the classpath for 'compile' to
  work.

  Defaults to \"classes\"")

(add-doc *compile-files*
  "Set to true when compiling files, false otherwise.")

(add-doc *ns*
  "A clojure.lang.Namespace object representing the current namespace.")

(add-doc *in*
  "A java.io.Reader object representing standard input for read operations.

  Defaults to System/in, wrapped in a LineNumberingPushbackReader")

(add-doc *out*
  "A java.io.Writer object representing standard output for print operations.

  Defaults to System/out")

(add-doc *err*
  "A java.io.Writer object representing standard error for print operations.

  Defaults to System/err, wrapped in a PrintWriter")

(add-doc *flush-on-newline*
  "When set to true, output will be flushed whenever a newline is printed.

  Defaults to true.")

(add-doc *print-meta*
  "If set to logical true, when printing an object, its metadata will also
  be printed in a form that can be read back by the reader.

  Defaults to false.")

(add-doc *print-dup*
  "When set to logical true, objects will be printed in a way that preserves
  their type when read in later.

  Defaults to false.")

(add-doc *print-readably*
  "When set to logical false, strings and characters will be printed with
  non-alphanumeric characters converted to the appropriate escape sequences.

  Defaults to true")

(add-doc *read-eval*
  "When set to logical false, the EvalReader (#=(...)) is disabled in the 
  read/load in the thread-local binding.
  Example: (binding [*read-eval* false] (read-string \"#=(eval (def x 3))\"))

  Defaults to true")

(defn future?
  "Returns true if x is a future"
  [x] (instance? clojure.lang.Future x))          ;;; java.util.concurrent.Future

(defn future-done?
  "Returns true if future f is done"
  [#^clojure.lang.Future f] (.isDone f))          ;;; #^java.util.concurrent.Future


(defmacro letfn 
  "Takes a vector of function specs and a body, and generates a set of
  bindings of functions to their names. All of the names are available
  in all of the definitions of the functions, as well as the body.

  fnspec ==> (fname [params*] exprs) or (fname ([params*] exprs)+)" 
  [fnspecs & body] 
  `(letfn* ~(vec (interleave (map first fnspecs) 
                             (map #(cons `fn %) fnspecs)))
           ~@body))

;;;;;;; case ;;;;;;;;;;;;;
(defn- shift-mask [shift mask x]
  (-> x (bit-shift-right shift) (bit-and mask)))

(defn- min-hash 
  "takes a collection of keys and returns [shift mask]"
  [keys]
  (let [hashes (map hash keys)
        cnt (count keys)]
    (when-not (apply distinct? hashes)
      (throw (ArgumentException. "Hashes must be distinct")))              ;;; IllegalArgumentException
    (or (first 
         (filter (fn [[s m]]
                   (apply distinct? (map #(shift-mask s m %) hashes)))
                 (for [mask (map #(dec (bit-shift-left 1 %)) (range 1 14))
                       shift (range 0 31)]
                   [shift mask])))
        (throw (ArgumentException. "No distinct mapping found")))))        ;;; IllegalArgumentException

(defmacro case 
  "Takes an expression, and a set of clauses.

  Each clause can take the form of either:

  test-constant result-expr

  (test-constant1 ... test-constantN)  result-expr

  The test-constants are not evaluated. They must be compile-time
  literals, and need not be quoted.  If the expression is equal to a
  test-constant, the corresponding result-expr is returned. A single
  default expression can follow the clauses, and its value will be
  returned if no clause matches. If no default expression is provided
  and no clause matches, an IllegalArgumentException is thrown.

  Unlike cond and condp, case does a constant-time dispatch, the
  clauses are not considered sequentially.  All manner of constant
  expressions are acceptable in case, including numbers, strings,
  symbols, keywords, and (Clojure) composites thereof. Note that since
  lists are used to group multiple constants that map to the same
  expression, a vector can be used to match a list if needed. The
  test-constants need not be all of the same type."

  [e & clauses]
  (let [ge (with-meta (gensym) {:tag Object})
        default (if (odd? (count clauses)) 
                  (last clauses)
                  `(throw (ArgumentException. (str "No matching clause: " ~ge))))     ;;; IllegalArgumentException
        cases (partition 2 clauses)
        case-map (reduce (fn [m [test expr]]
                           (if (seq? test)
                             (into m (zipmap test (repeat expr)))
                             (assoc m test expr))) 
                           {} cases)
        [shift mask] (if (seq case-map) (min-hash (keys case-map)) [0 0])
        
        hmap (reduce (fn [m [test expr :as te]]
                       (assoc m (shift-mask shift mask (hash test)) te))
                     (sorted-map) case-map)]
    `(let [~ge ~e]
       ~(condp = (count clauses)
          0 default
          1 default
          `(case* ~ge ~shift ~mask ~(key (first hmap)) ~(key (last hmap)) ~default ~hmap 
                        ~(every? keyword? (keys case-map)))))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; helper files ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(alter-meta! (find-ns 'clojure.core) assoc :doc "Fundamental library of the Clojure language") (load "core_clr")
(load "core_proxy")
(load "core_print")
(load "genclass")
(load "core_deftype")
(load "core/protocols")
;(load "gvec")

;; redefine reduce with internal-reduce
(defn reduce-new
  "f should be a function of 2 arguments. If val is not supplied,
  returns the result of applying f to the first 2 items in coll, then
  applying f to that result and the 3rd item, etc. If coll contains no
  items, f must accept no arguments as well, and reduce returns the
  result of calling f with no arguments.  If coll has only 1 item, it
  is returned and f is not called.  If val is supplied, returns the
  result of applying f to val and the first item in coll, then
  applying f to that result and the 2nd item, etc. If coll contains no
  items, returns val and f is not called."
  ([f coll]
     (if-let [s (seq coll)]
       (reduce f (first s) (next s))
       (f)))
  ([f start coll]
     (let [s (seq coll)]
       (clojure.core.protocols/internal-reduce s f start))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; futures (needs proxy);;;;;;;;;;;;;;;;;;
(defn future-call 
  "Takes a function of no args and yields a future object that will
  invoke the function in another thread, and will cache the result and
  return it on all subsequent calls to deref/@. If the computation has
  not yet finished, calls to deref/@ will block."
  [f]                                                 ;;;  [#^Callable f]
    (clojure.lang.Future. f))                         ;;;  (let [fut (.submit clojure.lang.Agent/soloExecutor f)]
;;;    (reify 
;;;     clojure.lang.IDeref 
;;;      (deref [_] (.get fut))
;;;     java.util.concurrent.Future
;;;      (get [_] (.get fut))
;;;      (get [_ timeout unit] (.get fut timeout unit))
;;;      (isCancelled [_] (.isCancelled fut))
;;;      (isDone [_] (.isDone fut))
;;;      (cancel [_ interrupt?] (.cancel fut interrupt?)))))
  
(defmacro future
  "Takes a body of expressions and yields a future object that will
  invoke the body in another thread, and will cache the result and
  return it on all subsequent calls to deref/@. If the computation has
  not yet finished, calls to deref/@ will block."  
  [& body] `(future-call (fn [] ~@body)))


(defn future-cancel
  "Cancels the future, if possible."
  [#^clojure.lang.Future f] (.cancel f true))    ;;; java.util.concurrent.Future

(defn future-cancelled?
  "Returns true if future f is cancelled"
  [#^clojure.lang.Future f] (.isCancelled f))    ;;; java.util.concurrent.Future

(defn pmap
  "Like map, except f is applied in parallel. Semi-lazy in that the
  parallel computation stays ahead of the consumption, but doesn't
  realize the entire result unless required. Only useful for
  computationally intensive functions where the time of f dominates
  the coordination overhead."
  ([f coll]
   (let [n (+ 2 Environment/ProcessorCount)            ;;; (.. Runtime getRuntime availableProcessors)
         rets (map #(future (f %)) coll)
         step (fn step [[x & xs :as vs] fs]
                (lazy-seq
                 (if-let [s (seq fs)]
                   (cons (deref x) (step xs (rest s)))
                   (map deref vs))))]
     (step rets (drop n rets))))
  ([f coll & colls]
   (let [step (fn step [cs]
                (lazy-seq
                 (let [ss (map seq cs)]
                   (when (every? identity ss)
                     (cons (map first ss) (step (map rest ss)))))))]
     (pmap #(apply f %) (step (cons coll colls))))))

(defn pcalls
  "Executes the no-arg fns in parallel, returning a lazy sequence of
  their values" 
  [& fns] (pmap #(%) fns))

(defmacro pvalues
  "Returns a lazy sequence of the values of the exprs, which are
  evaluated in parallel" 
  [& exprs]
  `(pcalls ~@(map #(list `fn [] %) exprs)))



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; clojure version number ;;;;;;;;;;;;;;;;;;;;;;
;;; THIS EXPOSES WAY TOO MUCH JVM INTERNALS!
(let [                                                                      ;;; version-stream (.getResourceAsStream (clojure.lang.RT/baseLoader) 
                                                                            ;;;                                      "clojure/version.properties")
      properties (. clojure.lang.RT GetVersionProperties)                   ;;; properties     (doto (new java.util.Properties) (.load version-stream))
      prop (fn [k] (.getProperty properties (str "clojure.version." k)))
      clojure-version {:major       (Int32/Parse #^String (prop "major"))            ;;;(Integer/valueOf (prop "major"))
                       :minor       (Int32/Parse #^String (prop "minor"))            ;;;(Integer/valueOf (prop "minor"))
                       :incremental (Int32/Parse #^String (prop "incremental"))      ;;;(Integer/valueOf (prop "incremental"))
                       :qualifier   (prop "qualifier")}]
  (def *clojure-version* 
    (if (not (= (prop "interim") "false"))
      (clojure.lang.RT/assoc clojure-version :interim true)
      clojure-version)))
      
(add-doc *clojure-version*
  "The version info for Clojure core, as a map containing :major :minor 
  :incremental and :qualifier keys. Feature releases may increment 
  :minor and/or :major, bugfix releases will increment :incremental. 
  Possible values of :qualifier include \"GA\", \"SNAPSHOT\", \"RC-x\" \"BETA-x\"")
      
(defn
  clojure-version 
  "Returns clojure version as a printable string."
  []
  (str (:major *clojure-version*)
       "."
       (:minor *clojure-version*)
       (when-let [i (:incremental *clojure-version*)]
         (str "." i))
       (when-let [q (:qualifier *clojure-version*)]
         (when (pos? (count q)) (str "-" q)))
       (when (:interim *clojure-version*)
         "-SNAPSHOT")))

(defn promise
  "Alpha - subject to change.
  Returns a promise object that can be read with deref/@, and set,
  once only, with deliver. Calls to deref/@ prior to delivery will
  block. All subsequent derefs will return the same delivered value
  without blocking."  
  [] 
  (let [d (clojure.lang.CountDownLatch. 1)                       ;;; java.util.concurrent.CountDownLatch.
        v (atom nil)]
    (proxy [clojure.lang.AFn clojure.lang.IDeref] []             ;;; TODO: Update this to reify after we have fixed the missing method problem.
      (deref [] (.Await d) @v) ;;; await
      (invoke [x]
        (locking d
          (if (pos? (.Count d)) ;;; getCount
            (do (reset! v x)
                (.CountDown d) ;;; countDown
                this)
            (throw (InvalidOperationException. "Multiple deliver calls to a promise")))))))) ;;; IllegalStateException;


        
(defn deliver
  "Alpha - subject to change.
  Delivers the supplied value to the promise, releasing any pending
  derefs. A subsequent call to deliver on a promise will throw an exception."  
  [promise val] (promise val))

;;;;;;;;;;;;;;;;;;;;; editable collections ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn transient 
  "Alpha - subject to change.
  Returns a new, transient version of the collection, in constant time."
  [#^clojure.lang.IEditableCollection coll] 
  (.asTransient coll))

(defn persistent! 
  "Alpha - subject to change.
  Returns a new, persistent version of the transient collection, in 
  constant time. The transient collection cannot be used after this
  call, any such use will throw an exception."
  [#^clojure.lang.ITransientCollection coll] 
  (.persistent coll))

(defn conj!
  "Alpha - subject to change.
  Adds x to the transient collection, and return coll. The 'addition'
  may happen at different 'places' depending on the concrete type."
  [#^clojure.lang.ITransientCollection coll x]
  (.conj coll x))

(defn assoc!
  "Alpha - subject to change.
  When applied to a transient map, adds mapping of key(s) to
  val(s). When applied to a transient vector, sets the val at index.
  Note - index must be <= (count vector). Returns coll."
  ([#^clojure.lang.ITransientAssociative coll key val] (.assoc coll key val))
  ([#^clojure.lang.ITransientAssociative coll key val & kvs]
   (let [ret (.assoc coll key val)]
     (if kvs
       (recur ret (first kvs) (second kvs) (nnext kvs))
       ret))))

(defn dissoc!
  "Alpha - subject to change.
  Returns a transient map that doesn't contain a mapping for key(s)."
  ([#^clojure.lang.ITransientMap map key] (.without map key))
  ([#^clojure.lang.ITransientMap map key & ks]
   (let [ret (.without map key)]
     (if ks
       (recur ret (first ks) (next ks))
       ret))))

(defn pop!
  "Alpha - subject to change.
  Removes the last item from a transient vector. If
  the collection is empty, throws an exception. Returns coll"
  [#^clojure.lang.ITransientVector coll] 
  (.pop coll))  

(defn disj!
  "Alpha - subject to change.
  disj[oin]. Returns a transient set of the same (hashed/sorted) type, that
  does not contain key(s)."
  ([set] set)
  ([#^clojure.lang.ITransientSet set key]
   (. set (disjoin key)))
  ([set key & ks]
   (let [ret (disj set key)]
     (if ks
       (recur ret (first ks) (next ks))
       ret))))

;redef into with batch support
(defn into
  "Returns a new coll consisting of to-coll with all of the items of
  from-coll conjoined."
  [to from]
  (if (instance? clojure.lang.IEditableCollection to)
    (#(loop [ret (transient to) items (seq from)]
        (if items
          (recur (conj! ret (first items)) (next items))
          (persistent! ret))))
    (#(loop [ret to items (seq from)]
        (if items
          (recur (conj ret (first items)) (next items))
          ret)))))