(ns spider-game.scenes.level-01
  (:require [clunk.collision :as collision]
            [clunk.core :as c]
            [clunk.input :as i]
            [clunk.scene :as scene]
            [clunk.sprite :as sprite]
            [clunk.tween :as tween]
            [clunk.util :as u]
            [clunk.delay :as delay]
            [spider-game.common :as common]
            [spider-game.scenes.bite-overlay :as bite-overlay]
            [spider-game.sprites.fly :as fly]
            [spider-game.sprites.player-spider :as ps]
            [spider-game.sprites.web :as web]
            [clunk.shape :as shape]
            [clunk.palette :as p]
            [spider-game.sprites.web-break :as web-break]
            [spider-game.sprites.web-fix :as web-fix]
            [spider-game.scenes.repair-overlay :as repair-overlay]
            [spider-game.sprites.score-box :as score-box]
            [spider-game.sprites.round-timer :as round-timer])
  (:import (org.lwjgl.glfw GLFW)))

(defn flies
  [n window]
  (repeatedly n #(fly/fly (u/window-pos window [(rand) (rand)]))))

(defn sprites
  "The initial list of sprites for this scene"
  [{:keys [window] :as state}]
  (concat 
   [(web/web window)]
   ;;   (flies 3 window)
   [(ps/spider (u/center window))
    (score-box/score-box)
    (round-timer/round-timer (u/window-pos window [0.507 0.067]) (assoc p/black 3 0.4))
    (round-timer/round-timer (u/window-pos window [0.5 0.06]) p/white)]))

;; @TODO: tell clunk which order to draw sprites in
(defn draw-level-01!
  "Called each frame, draws the current scene to the screen"
  [{:keys [current-scene] :as state}]
  (c/draw-background! common/dark-green)
  ;; draw web first
  (let [sprites (get-in state [:scenes current-scene :sprites])
        [[web] others] (u/split-by (sprite/has-group :web) sprites)
        draw-web! (:draw-fn web)]
    (draw-web! state web)

    ;; then draw the rest
    (doall
     (map (fn [{:keys [draw-fn debug?] :as s}]
            (draw-fn state s)
            (when debug?
              (sprite/draw-bounds state s)
              (sprite/draw-center state s)))
          others))))

(defn handle-escapes
  [{:keys [current-scene] :as state}]
  (let [flies (filter (sprite/has-group :fly)
                      (get-in state [:scenes current-scene :sprites]))]
    (reduce (fn [acc-state f]
              (if (and (not (pos? (:remaining f)))
                       (= :struggling (:status f)))
                (-> acc-state
                    (sprite/update-sprites
                     (sprite/is-sprite f)
                     fly/begin-escape)
                    (sprite/update-sprites
                     (sprite/has-group :web)
                     (fn [w]
                       (web/break-web-at w (:pos f)))))
                acc-state))
            state
            flies)))

(defn remove-escaped
  [{:keys [current-scene] :as state}]
  (update-in state [:scenes current-scene :sprites]
             (fn [sprites]
               (remove (fn [s]
                         (and ((sprite/has-group :fly) s)
                              (= :escaped (:status s))))
                       sprites))))

(defn remove-flagged
  [{:keys [current-scene] :as state}]
  (update-in state [:scenes current-scene :sprites]
             (fn [sprites]
               (remove (fn [s]
                         (= :remove-me (:status s)))
                       sprites))))

(defn end-transition
  [{:keys [window current-scene scenes] :as state}
   target
   i
   transition-length]
  (let [rc-title (get-in state [:scenes current-scene :rc-title])
        [_x start-y] (:pos rc-title)
        end-y 100
        dy (- end-y start-y)
        progress (float (/ i (/ transition-length 2)))
        tweened-progress (tween/ease-out-expo progress)
        current-y (* dy tweened-progress)
        rc-title(update-in rc-title [:pos 1] + current-y)]
    (if (< i (/ transition-length 2))
      (do
        (c/draw-game! (assoc state :current-scene current-scene))
        (shape/fill-rect!
         state
         [0 0]
         (u/window-size window)
         [0 0 0 (float (/ (* 2 i) transition-length))])
        (sprite/draw-text-sprite! state rc-title)
        (GLFW/glfwSwapBuffers window))
      (do
        (c/draw-game! (assoc state :current-scene target))
        (shape/fill-rect!
         state
         [0 0]
         (u/window-size window)
         [0 0 0 (float (/ (* 2 (- transition-length i)) transition-length))])
        (sprite/draw-text-sprite! state rc-title)
        (GLFW/glfwSwapBuffers window)))))

(defn draw-text-sprite-with-drop-shadow
  [state text-sprite]
  (sprite/draw-text-sprite! state
                            (-> text-sprite
                                ;; @TODO: make [7 7] relative to font size?
                                (update :pos #(map + [7 7] %))
                                (assoc :color (assoc p/black 3 0.4))))
  (sprite/draw-text-sprite! state
                            text-sprite))

(defn transition-end
  [{:keys [current-scene] :as state}]
  (let [rc-title (first (filter (sprite/has-group :round-clear-title)
                                (get-in state [:scenes current-scene :sprites])))]
    (-> state
        (assoc-in [:scenes current-scene :rc-title]
                  rc-title)
        (update-in [:scenes current-scene :sprites]
                   #(remove (sprite/has-group :round-clear-title) %))
        (scene/transition :round-end
                          :transition-fn end-transition
                          :transition-length 100
                          :init-fn (fn [st]
                                     (update-in st [:scenes :round-end :sprites]
                                                conj
                                                (assoc-in rc-title [:pos 1] 100)))))))

(defn check-end
  [{:keys [window current-scene] :as state}]
  (let [timer (first (filter (sprite/has-group :timer)
                             (get-in state [:scenes current-scene :sprites])))]
    (if (and (not= :ending (get-in state [:scenes current-scene :status]))
             (:remaining-ms timer)
             (zero? (:remaining-ms timer)))
      ;; @TOOD: clunk text-sprite should have a drop-shadow option
      (-> state
          (update-in [:scenes current-scene :sprites]
                     conj
                     (sprite/text-sprite :round-clear-title
                                         (u/window-pos window [0.5 0.9])
                                         "Round Clear!"
                                         :font-size 96
                                         :draw-fn draw-text-sprite-with-drop-shadow))
          (assoc-in [:scenes current-scene :status] :ending)
          (delay/add-delay-fn
           (delay/delay-fn 1500 transition-end)))
      state)))

(defn update-level-01
  "Called each frame, update the sprites in the current scene"
  [state]
  (-> state
      round-timer/update-state
      handle-escapes
      remove-escaped
      remove-flagged
      sprite/update-state
      tween/update-state
      delay/update-state
      collision/update-state
      check-end))

(defn colliders
  []
  [(collision/collider
    :fly
    :player-spider
    (fn [{:keys [status] :as fly} _spider]
      (if (= :struggling status)
        (sprite/set-animation fly :biteable)
        fly))
    collision/identity-collide-fn
    :non-collide-fn-a
    (fn [{:keys [status] :as fly} _spider]
      (if (= :struggling status)
        (sprite/set-animation fly :struggling)
        fly)))])

;; @NOTE: helpful debugging function
(defn break-web-on-click
  [state {:keys [pos] :as e}]
  (if (i/is e :button i/M_LEFT)
    (sprite/update-sprites
     state
     (sprite/has-group :web)
     (fn [w]
       (web/break-web-at w pos)))
    state))

;; @NOTE: helpful debugging function
(defn repair-on-click
  [state {:keys [pos] :as e}]
  (if (i/is e :button i/M_RIGHT)
    ;; This is for using he repair overlay, but honestly it's just not that fun.
    ;; ;; @TODO: get check nearest first
    ;; (scene/transition
    ;;  state
    ;;  :repair-overlay
    ;;  :transition-length 0
    ;;  :init-fn (fn [state]
    ;;             (assoc-in state
    ;;                       [:scenes :repair-overlay]
    ;;                       ;; ensure the connections are randomised
    ;;                       (assoc (repair-overlay/init state)
    ;;                              ;; also insert the repair pos so we
    ;;                              ;; know where to repair when we're
    ;;                              ;; done
    ;;                              :repair-pos pos))))
    (sprite/update-sprites
     state
     (sprite/has-group :web)
     (fn [w]
       (web/fix-web-at w pos)))
    state))

(defn move-spider-on-click
  [state {:keys [pos] :as e}]
  (if (i/is e :action i/PRESS)
    (sprite/update-sprites
     state
     (sprite/has-group :player-spider)
     #(ps/move % pos))
    state))

(defn bite-on-space
  [{:keys [current-scene] :as state} e]
  ;; @TODO: temp for ease of testing
  (cond
    (i/is e :key i/K_SPACE)
    (let [sprites (get-in state [:scenes current-scene :sprites])
          spider (first (filter (sprite/has-group :player-spider) sprites))
          flies (filter (sprite/has-group :fly) sprites)
          target-fly (first (filter #(and (#{:biteable :struggling} (:current-animation %1))
                                          (collision/w-h-rects-collide? %1 spider))
                                    flies))]
      (if target-fly
        (scene/transition
         state
         :bite-overlay
         :transition-length 0
         :init-fn (fn [state]
                    (assoc-in state
                              [:scenes :bite-overlay]
                              ;; ensure the goal is in a new random position
                              (assoc (bite-overlay/init state)
                                     ;; also insert the fly uuid so we
                                     ;; know which sprite to wrap once
                                     ;; we're done
                                     :fly-uuid (:uuid target-fly)))))
        state))

    :else
    state))

;; @TODO: need to use up some kind of resources, need to indicatew that spider is in range of repair
(defn repair-web-on-r
  [{:keys [current-scene] :as state} {:keys [pos] :as e}]
  (if (i/is e :key i/K_R)
    (let [sprites (get-in state [:scenes current-scene :sprites])
          spider (first (filter (sprite/has-group :player-spider) sprites))]
      (sprite/update-sprites
       state
       (sprite/has-group :web)
       (fn [w]
         (web/fix-web-at w (:pos spider)))))
    state))

(defn spawn-web-break
  [{:keys [current-scene] :as state} {:keys [source threads]}]
  (update-in state
             [:scenes current-scene :sprites]
             conj
             (web-break/web-break source threads)))

(defn spawn-web-fix
  [{:keys [current-scene] :as state} {:keys [source threads]}]
  (let [wf (web-fix/web-fix source threads)]
    (update-in state
               [:scenes current-scene :sprites]
               conj
               wf)))

(defn complete-fix
  [state {:keys [pos]}]
  (sprite/update-sprites
   state
   (sprite/has-group :web)
   (fn [w]
     (web/complete-fix w pos))))

(def initial-spawn-delay-ms 8000)
(def min-spawn-delay-ms 1500)
(def max-flies 4)
(def no-fly-zone 100)

(defn random-fly-pos
  [window]
  (let [[w h] (u/window-size window)]
    [(+ no-fly-zone
        (rand-int (- w (* no-fly-zone 2))))
     (+ no-fly-zone
        (rand-int (- h (* no-fly-zone 2))))]))

(defn spawn-flies
  [{:keys [window current-scene] :as state}]
  (let [spawn-delay-ms (get-in state [:scenes current-scene :spawn-delay-ms])
        flies (filter (fn [f] (and (= :struggling (:status f))
                                   ((sprite/has-group :fly) f)))
                      (get-in state [:scenes current-scene :sprites]))]
    (if (< (count flies) max-flies)
      ;; @TODO: slowly increase max flies too? maybe up to like 9?
      ;; spawn a fly, speed up, repeat
      (-> state
          (update-in [:scenes current-scene :sprites]
                     #(conj % (fly/fly (random-fly-pos window))))
          (delay/add-delay-fn
           (delay/delay-fn spawn-delay-ms
                           spawn-flies
                           :tag :spawn-flies))
          (update-in [:scenes current-scene :spawn-delay-ms]
                     #(max min-spawn-delay-ms (- % 1000))))

      ;; too many flies, don't spawn, don't speed up, just repeat
      (delay/add-delay-fn
       state
       (delay/delay-fn spawn-delay-ms
                       spawn-flies
                       :tag :spawn-flies)))))

(defn initial-delays
  [{:keys [current-scene] :as state}]
  [(delay/delay-fn
    3000
    spawn-flies
    :tag :initial-spawn-flies)])

(defn inc-score
  [state {:keys [fly]}]
  (sprite/update-sprites
   state
   (sprite/has-group :score-box)
   (fn [{:keys [show-fly?] :as sb}]
     (if (not show-fly?)
       (-> sb
           (assoc :fly fly)
           (assoc :show-fly? true)
           (update :score inc))
       (update sb :score inc)))))

(defn init
  "Initialise this scene"
  [state]
  {:sprites (sprites state)
   :draw-fn draw-level-01!
   :update-fn update-level-01
   :colliders (colliders)
   :mouse-button-fns [move-spider-on-click
                      ;; break-web-on-click
                      ;; repair-on-click
                      ]
   :key-fns [bite-on-space
             repair-web-on-r]
   :spawn-web-break-fns [spawn-web-break]
   :spawn-web-fix-fns [spawn-web-fix]
   :complete-fix-fns [complete-fix]
   :delay-fns (initial-delays state)
   :spawn-delay-ms initial-spawn-delay-ms
   :inc-score-fns [inc-score]})
