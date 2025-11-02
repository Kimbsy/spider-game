(ns spider-game.scenes.wrap-overlay
  (:require [clunk.util :as u]
            [clunk.sprite :as sprite]
            [spider-game.common :as common]
            [clunk.palette :as p]
            [clunk.core :as c]
            [clunk.tween :as tween]
            [clunk.input :as i]
            [clunk.scene :as scene]
            [spider-game.sprites.fly :as fly]
            [clunk.shape :as shape]
            [clunk.collision :as collision]))

(def wraps-required 5)

(defn sprites
  [{:keys [window] :as state}]
  (let [[w h] (u/window-size window)
        [cx cy :as center] (u/center window)
        shadow-pad 15
        hw-pad 200
        vw-pad 200
        c-pad 10
        bgw (- w (* hw-pad 2))
        bgh (- h (* vw-pad 2))
        fgw (- bgw c-pad)
        fgh (- bgh c-pad)
        iw (- fgw c-pad)
        ih (- fgh c-pad)]
    (concat
     ;; @TODO: add drop shadow?
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
      (fly/fly center)])))

(defn draw-level-01!
  [state]
  (let [{:keys [draw-fn sprites]} (get-in state [:scenes :level-01])]
    (doall
     (map (fn [{:keys [draw-fn debug?] :as s}]
            (draw-fn state s)
            (when debug?
              (sprite/draw-bounds state s)
              (sprite/draw-center state s)))
          sprites))))

(defn draw-wrap-overlay!
  [{:keys [window current-scene] :as state}]
  ;; draw level-01 scene
  (c/draw-background! common/dark-green)
  (draw-level-01! state)

  (sprite/draw-scene-sprites! state)

  ;; @TODO: make the wrapping look a bit nicer. Could animate the
  ;; threads as they appear, show the mouse-connected one as a ghost,
  ;; have a spider leg move the thread? Once completed could tween the
  ;; points to next-to the fly. Could not allow points inside the fly
  ;; hitbox?
  (let [set-points (get-in state [:scenes current-scene :wrap-points])
        wrapping? (= :wrapping (get-in state [:scenes current-scene :status]))
        wrap-points (if wrapping?
                      (conj set-points
                            (u/mouse-pos window))
                      set-points)
        lines (partition 2 1 wrap-points)]
    (shape/draw-lines! state lines (if wrapping? common/spider-white p/green) :line-width 3)))

(defn line-hits-rect?
  [[a b c d :as _rect-points] line]
  (or (collision/lines-intersect? line [a b])
      (collision/lines-intersect? line [b c])
      (collision/lines-intersect? line [c d])
      (collision/lines-intersect? line [d a])))

(defn wrap-level-01-fly
  [state uuid]
  (update-in state [:scenes :level-01 :sprites]
             (fn [sprites]
               (pmap (fn [f]
                       (if (= uuid (:uuid f))
                         (sprite/set-animation f :wrapped)
                        f))
                     sprites))))

(defn check-wrapped
  [{:keys [window current-scene] :as state}]
  (let [wrap-points (get-in state [:scenes current-scene :wrap-points])
        lines (partition 2 1 wrap-points)
        sprites (get-in state [:scenes current-scene :sprites])
        {:keys [pos bounds-fn] :as fly} (first (filter (sprite/has-group :fly) sprites))
        pos-offsets (sprite/pos-offsets fly)
        rect (map (partial map + pos pos-offsets) (bounds-fn fly))]
    (if (<= wraps-required (count (filter (partial line-hits-rect? rect) lines)))
      (-> state
          (assoc-in [:scenes current-scene :status] :wrapped)
          (wrap-level-01-fly (get-in state [:scenes current-scene :fly-uuid]))
          (scene/transition :level-01))
      state)))

(defn update-wrap-overlay
  [state]
  (-> state
      tween/update-state
      check-wrapped))

(defn esc
  [state e]
  (if (i/is e :key i/K_ESCAPE)
    (scene/transition state :level-01 :transition-length 0)
    state))

(defn click
  [{:keys [current-scene] :as state} e]
  (if (and (= :wrapping (get-in state [:scenes current-scene :status]))
           (i/is e :button i/M_LEFT))
    (update-in state [:scenes current-scene :wrap-points] conj (:pos e))
    state))

(defn init
  "Initialise this scene"
  [state]
  {:sprites (sprites state)
   :draw-fn draw-wrap-overlay!
   :update-fn update-wrap-overlay
   :mouse-button-fns [click]
   :key-fns [esc]
   :status :wrapping
   :wrap-points []})
