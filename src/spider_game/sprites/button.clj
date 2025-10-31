(ns spider-game.sprites.button
  (:require [clunk.shape :as shape]
            [clunk.sprite :as sprite]
            [clunk.text :as text]
            [clunk.palette :as p]))

(def title-text-size 120)
(def large-text-size 50)
(def button-teal [0.0 0.5882353 0.654902 1])

(defn draw-button
  [state
   {:keys [content content-color font-size size bg-color offsets]
    [x0 y0 :as pos] :pos
    [bw bh :as button-size] :size
    :as button}]
  (let [[x y :as offset-pos] (mapv + pos (sprite/pos-offsets button))
        [tw th :as text-size] [(* (count content) font-size 0.5) font-size]
        text-pos [(- (+ x (/ bw 2))
                     (/ tw 4))
                  (+ (+ y (/ bh 2))
                     (/ th 4))]]
    (shape/fill-rect! state offset-pos size bg-color)
    (text/draw-text! state text-pos content :color content-color)))

(defn button-sprite
  [pos content & {:keys [font-size] :or {font-size large-text-size}}]
  (sprite/sprite :button
                 pos
                 :size [200 100]
                 :draw-fn draw-button
                 :draw-requires-state? true
                 :extra {:content content
                         :font-size font-size
                         :content-color p/grey
                         :bg-color p/white}))
