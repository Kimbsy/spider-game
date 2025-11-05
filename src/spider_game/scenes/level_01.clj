(ns spider-game.scenes.level-01
  (:require [clunk.collision :as collision]
            [clunk.core :as c]
            [clunk.input :as i]
            [clunk.scene :as scene]
            [clunk.sprite :as sprite]
            [clunk.tween :as tween]
            [clunk.util :as u]
            [spider-game.common :as common]
            [spider-game.scenes.bite-overlay :as bite-overlay]
            [spider-game.sprites.fly :as fly]
            [spider-game.sprites.player-spider :as ps]
            [spider-game.sprites.web :as web]
            [clunk.shape :as shape]
            [clunk.palette :as p]
            [spider-game.sprites.web-break :as web-break]
            [spider-game.sprites.web-fix :as web-fix]
            [spider-game.scenes.repair-overlay :as repair-overlay]))

(defn flies
  [n window]
  (repeatedly n #(fly/fly (u/window-pos window [(rand) (rand)]))))

(defn sprites
  "The initial list of sprites for this scene"
  [{:keys [window] :as state}]
  (concat 
   [(web/web window)]
   (flies 3 window)
   [(ps/spider (u/center window))]))

(defn draw-level-01!
  "Called each frame, draws the current scene to the screen"
  [state]
  (c/draw-background! common/dark-green)
  (sprite/draw-scene-sprites! state))

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

(defn update-level-01
  "Called each frame, update the sprites in the current scene"
  [state]
  (-> state
      handle-escapes
      remove-escaped
      remove-flagged
      sprite/update-state
      tween/update-state
      ((fn [state]
         (sprite/update-sprites
          state
          (sprite/has-group :player-spider)
          (fn [s]
            (assoc s :debug? false)))))
      collision/update-state
      ))

;; @TODO: not working, could do with a "no collisions fn", otherwise we trigger non-hit for each fly that doesn't collide
(defn colliders
  []
  []
  #_[(collision/collider
    :player-spider
    :fly
    (fn [spider _fly]
      (assoc spider :debug? true))
    collision/identity-collide-fn)])

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
          target-fly (first (filter #(and (= :struggling (:current-animation %1))
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

;; @TODO: need to animate repair, need to use up some kind of resources, need to indicatew that spider is in range of repair
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
   :complete-fix-fns [complete-fix]})
