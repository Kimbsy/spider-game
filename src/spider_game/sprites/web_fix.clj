(ns spider-game.sprites.web-fix
  (:require [clunk.sprite :as sprite]
            [clunk.shape :as shape]
            [spider-game.common :as common]
            [clunk.core :as c]
            [clunk.tween :as tween]))

(def fix-frames 40)

(defn progress-line
  [progress {:keys [start destination]}]
  (let [[dx dy] (map - destination start)
        ratio (tween/ease-in-out-expo (/ progress fix-frames))
        delta [(* dx ratio) (* dy ratio)]
        end (mapv + start delta)]
    [start end]))

(defn draw-web-fix!
  [state {:keys [status threads progress]}]
  (when (not= :remove-me status)
    (let [lines (map (partial progress-line progress) threads)]
      (shape/draw-lines! state lines common/silk-white))))

(defn update-web-fix
  [{:keys [progress source] :as wf}]
  (if (<= fix-frames progress)
    (do
      (c/enqueue-event! {:event-type :complete-fix
                         :pos source})
      (assoc wf :status :remove-me))
    (update wf :progress inc)))

(defn prep-thread
  [source [a b]]
  (merge
   {:destination source}
   ;; either `a` or `b` will be `source`
   (if (= source a)
     {:start b}
     {:start a})))

(defn web-fix
  [source threads]
  (sprite/sprite
   :web-fix
   source
   :update-fn update-web-fix
   :draw-fn draw-web-fix!
   :extra {:source source
           :threads (map (partial prep-thread source) threads)
           :progress 0}))
