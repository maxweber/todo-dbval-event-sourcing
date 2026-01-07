(ns ui.register
  (:require [parts.replicant.basic]
            [parts.replicant.command-query]
            [ui.todo-page]))

(def register
  (concat
   (parts.replicant.basic/get-all)
   parts.replicant.command-query/register
   ui.todo-page/register))
