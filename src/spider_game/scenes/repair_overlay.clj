(ns spider-game.scenes.repair-overlay
  (:require [clunk.core :as c]
            [spider-game.common :as common]
            [clunk.sprite :as sprite]
            [clunk.util :as u]
            [clunk.palette :as p]
            [clunk.input :as i]
            [clunk.scene :as scene]
            [spider-game.sprites.web :as web]
            [clunk.text :as text]
            [clunk.tween :as tween]
            [clunk.shape :as shape]))

(defn draw-letter!
  [state pos ch]
  (text/draw-text! state pos ch))

(defn draw-selected-letter!
  [state pos ch]
  (text/draw-text! state pos ch :color p/yellow))

(defn draw-connected-letter!
  [state pos ch]
  (text/draw-text! state pos ch :color p/green))

(defn draw-connections!
  [{:keys [window] :as state}
   {:keys [letters current-selection required-links status] :as connection}]
  (doseq [[k {:keys [pos ch status] :as letter}] letters]
    (let [draw-fn (case status
                    :unconnected draw-letter!
                    :selected draw-selected-letter!
                    :connected draw-connected-letter!)]
      (draw-fn state pos ch)))

  ;; @TODO: draw requirements

  ;; @TODO: animate lines
  ;; draw connection lines
  (doseq [[k {:keys [pos status] :as letter}] letters]
    (when (= :connected status)
      (shape/draw-line! state
                        pos
                        (:pos (letters (required-links k)))
                        p/white
                        :line-width 4)))
  )

(defn update-connections
  [c]
  c)

(defn offset-pos
  "offsetting font size 32 single cars"
  [[w h] [x-factor y-factor]]
  [(- (* w x-factor) 16)
   (+ (* h y-factor) 16)])

(defn random-links
  []
  ;; @TODO: actually randomize
  {:W :Z
   :E :X
   :R :C
   :T :V
   :Z :W
   :X :E
   :C :R
   :V :T})

(defn letter
  [ch pos]
  {:pos pos
   :ch ch
   :status :unconnected})

(defn connections
  [ws]
  (sprite/sprite
   :connections
   [0 0]
   :draw-fn draw-connections!
   :update-fn update-connections
   :extra {:letters {:W (letter "W" (offset-pos ws [1/5 0.25]))
                     :E (letter "E" (offset-pos ws [2/5 0.25]))
                     :R (letter "R" (offset-pos ws [3/5 0.25]))
                     :T (letter "T" (offset-pos ws [4/5 0.25]))
                     :Z (letter "Z" (offset-pos ws [1/5 0.75]))
                     :X (letter "X" (offset-pos ws [2/5 0.75]))
                     :C (letter "C" (offset-pos ws [3/5 0.75]))
                     :V (letter "V" (offset-pos ws [4/5 0.75]))}
           :current-selection nil
           :required-links (random-links)
           :status :waiting}))

(defn handle-key-press
  [{:keys [current-scene] :as state} k]
  (let [sprites (get-in state [:scenes current-scene :sprites])
        {:keys [letters
                current-selection
                required-links
                status]}
        (first (filter (sprite/has-group :connections) sprites))]
    (cond
      (= :connected (:status (letters k)))
      state
      
      (= :waiting status)
      (sprite/update-sprites state (sprite/has-group :connections)
                             (fn [connections]
                               (-> connections
                                   (assoc :current-selection k)
                                   (assoc :status :connecting)
                                   (assoc-in [:letters k :status] :selected))))
      (= :connecting status)
      (if (= k (required-links current-selection))
        (sprite/update-sprites state (sprite/has-group :connections)
                               (fn [connections]
                                 (-> connections
                                     (assoc :current-selection nil)
                                     (assoc :status :waiting)
                                     (assoc-in [:letters k :status] :connected)
                                     (assoc-in [:letters current-selection :status] :connected))))
        state)

      :else state)))

(defn sprites
  [{:keys [window] :as state}]
  (let [[w h :as ws] (u/window-size window)
        [cx cy :as center] (u/center window)
        shadow-pad 15
        hw-pad 100
        vw-pad 100
        c-pad 10
        bgw (- w (* hw-pad 2))
        bgh (- h (* vw-pad 2))
        fgw (- bgw c-pad)
        fgh (- bgh c-pad)
        iw (- fgw c-pad)
        ih (- fgh c-pad)]
    (concat
     [(sprite/geometry-sprite :shadow
                              [(+ cx shadow-pad) (+ cy shadow-pad)]
                              [[0 0]
                               [0 bgh]
                               [bgw bgh]
                               [bgw 0]]
                              :size [bgw bgh]
                              :color (assoc p/black 3 0.4)
                              :fill? true)
      (sprite/geometry-sprite :bg-container
                              center
                              [[0 0]
                               [0 bgh]
                               [bgw bgh]
                               [bgw 0]]
                              :size [bgw bgh]
                              :color p/grey
                              :fill? true)
      (sprite/geometry-sprite :container
                              center
                              [[0 0]
                               [0 fgh]
                               [fgw fgh]
                               [fgw 0]]
                              :size [fgw fgh]
                              :color common/spider-white
                              :fill? true)
      (sprite/geometry-sprite :inner
                              center
                              [[0 0]
                               [0 ih]
                               [iw ih]
                               [iw 0]]
                              :size [iw ih]
                              :color p/grey
                              :fill? true)
      (connections ws)])))

(defn draw-repair-overlay!
  [state]
  (c/draw-background! common/dark-green)
  (common/draw-level-01! state)

  (sprite/draw-scene-sprites! state))

(defn fix-level-01-web
  [state repair-pos]
  (update-in state [:scenes :level-01 :sprites]
             (fn [sprites]
               (pmap (fn [s]
                       (if (= :web (:sprite-group s))
                         (web/fix-web-at s repair-pos)
                         s))
                     sprites))))

(defn check-finished
  [{:keys [current-scene] :as state}]
  (let [sprites (get-in state [:scenes current-scene :sprites])
        {:keys [letters]} (first (filter (sprite/has-group :connections) sprites))]
    (if (every? #(= :connected (:status %)) (vals letters))
      (let [repair-pos (get-in state [:scenes current-scene :repair-pos])]
        (-> state
            (fix-level-01-web repair-pos)
            (scene/transition :level-01)))
      state)))

(defn update-repair-overlay
  [state]
  (-> state
      tween/update-state
      check-finished))

(def allowed? #{i/K_W i/K_E i/K_R i/K_T i/K_Z i/K_X i/K_C i/K_V})

(def convert {i/K_W :W
              i/K_E :E
              i/K_R :R
              i/K_T :T
              i/K_Z :Z
              i/K_X :X
              i/K_C :C
              i/K_V :V})

(defn key-pressed
  [state {:keys [k action] :as e}]
  (if (and (= i/PRESS action)
           (allowed? k))
    (handle-key-press state (convert k))
    state))

(defn init
  "Initialise this scene"
  [state]
  {:sprites (sprites state)
   :draw-fn draw-repair-overlay!
   :update-fn update-repair-overlay
   :key-fns [key-pressed]})
