(ns basket-sim.core
  (:require [clojure.java.io :as io]
            [clojure.data.csv :as csv])
  (:gen-class))


;;Path to a CSV file containing player data
(def player-data-path "../players_final.csv")

;;Function that reads the data from a CSV file and parses it to a list of player maps
(defn load-players 
  "Loads all of the players available for a draft from a csv file"
  [filepath]
  (with-open [reader (io/reader filepath)]
    (let [[header & rows] (csv/read-csv reader)
          keys (mapv keyword header)]
      (sort #(> (:per %1) (:per %2)) (mapv (fn [row]
              (let [player (zipmap keys row)
                    get-stat (fn [key] (Double/parseDouble (get player key "0.0")))]
                {:name (get player :Player)
                 :usage-rate (get-stat :USG%)
                 :shot-dist-2p (get-stat :2P_shot)
                 :shot-dist-3p (get-stat :3P_shot)
                 :fg-perc-2p (get-stat :2P%)
                 :fg-perc-3p (get-stat :3P%)
                 :orb-perc (get-stat :ORB%)
                 :drb-perc (get-stat :DRB%)
                 :ast-perc-2p (get-stat :2P.2)
                 :ast-perc-3p (get-stat :3P.2)
                 :ast-perc (get-stat :AST%)
                 :tov-pct (get-stat :TOV%)
                 :ftr (get-stat :FTr)
                 :ft-perc (get-stat :FT%)
                 :obpm (get-stat :OBPM)
                 :dbpm (get-stat :DBPM)
                 :per (get-stat :PER▼)}))
            rows)))))

(def draft-order [:pg :sg :sf :pf :c :bench :bench :bench :bench :bench])

(defn initial-draft-state
  [all-players per-cap]
  {:available-players all-players
   :draft-order draft-order
   :per-cap per-cap
   :current-pick 0
   :turn :team-a
   :teams {:team-a {:name (do
                            (println "Unesite ime prvog tima")
                            (read-line))
                    :on-court {}
                    :bench []
                    :per-left per-cap}
           :team-b {:name (do
                            (println "Unesite ime drugog tima")
                            (read-line))
                    :on-court {}
                    :bench []
                    :per-left per-cap}}})


(defn print-draft-state
  [draft-state]
  (Thread/sleep 1000)
  (println "Currently picking for team: " (get-in draft-state [:teams (:turn draft-state) :name]))
  (Thread/sleep 1000)
  (println "Picking for position: " (nth (:draft-order draft-state) (quot (:current-pick draft-state) 2)))
  (Thread/sleep 1000)
  (println "Remaining PER: " (get-in draft-state [:teams (:turn draft-state) :per-left]))
  (Thread/sleep 1000)
  (println "Available players:")
  (doseq [[index player] (map-indexed vector (:available-players draft-state))]
    (println index ". " (:name player) " PER: " (:per player))))

(print-draft-state (initial-draft-state (sort #(> (:per %1) (:per %2)) (load-players player-data-path)) 160))

;; Helper function to add the initial box score to a player
(defn with-initial-box-score [player]
  (assoc player
         :stamina 100.0
         :box-score {:pts 0, :ast 0, :reb 0, :fga 0, :fgm 0, :3pa 0, :3pm 0, :fta 0, :ftm 0, :tov 0}))

(defn apply-per-penalty [team]
  (let [penalty-fn (fn [p]
                     (-> p
                         (update :obpm + (:per-left team))
                         (update :dbpm + (:per-left team))))]
    (if (< (:per-left team) 0)
      (do
        (println "Applying a penalty to : " (:name team) ". Penalty : " (:per-left team))
       (-> team
          (assoc :on-court (into {} (map (fn [[pos p]]
                                           [pos (penalty-fn p)])
                                         (:on-court team))))
          (assoc :bench (mapv penalty-fn (:bench team)))))
      team)))

(defn run-draft
  [all-players per-cap]
  (loop [draft-state (initial-draft-state all-players per-cap)]
    (if (>= (:current-pick draft-state) (* 2 (count (:draft-order draft-state))))
      (let [final-teams (:teams draft-state)]
        (-> final-teams
            (update :team-a apply-per-penalty)
            (update :team-b apply-per-penalty)))
      (let [turn (:turn draft-state)
            opponent (if (= turn :team-a) :team-b :team-a)
            current-team (get-in draft-state [:teams turn])
            current-pick (:current-pick draft-state)
            draft-round (quot current-pick 2)
            current-pos (nth (:draft-order draft-state) draft-round)]
        (print-draft-state draft-state)
        (println "Pick a player by a number:")
        (let [player-index (Integer/parseInt (read-line))
              chosen-player (nth (:available-players draft-state) player-index)]
          (println "You selected: " (:name chosen-player))
          (let [new-team (if (= current-pos :bench)
                           (update current-team :bench conj (with-initial-box-score chosen-player))
                           (assoc-in current-team [:on-court current-pos] (with-initial-box-score chosen-player)))
                new-team (update new-team :per-left - (:per chosen-player))
                new-available (vec (remove #(= % chosen-player) (:available-players draft-state)))]
            (recur (-> draft-state
                       (assoc-in [:teams turn] new-team)
                       (assoc :available-players new-available)
                       (update :current-pick inc)
                       (assoc :turn opponent)))))))))


(run-draft (load-players player-data-path) 160)

; Nuggets
;(def team-a
; {:name "Denver Nuggets"
;   :on-court (into {} (map (fn [[pos p]] [pos (with-initial-box-score p)])
;                           {:pg {:name "Jamal Murray"
;                                 :usage-rate 27.5, :shot-dist-2p 0.655, :shot-dist-3p 0.345,
;                                 :fg-perc-2p 0.523, :fg-perc-3p 0.425, :orb-perc 2.1, :drb-perc 12.5,
;                                 :ast-perc-2p 0.45, :ast-perc-3p 0.65, :ast-perc 30.1, :tov-pct 9.9, :ftr 0.226, :ft-perc 0.88, 
;                                 :obpm 4.3, :dbpm -0.7}
;                            :sg {:name "Kentavious Caldwell-Pope"
;                                 :usage-rate 12.8, :shot-dist-2p 0.435, :shot-dist-3p 0.565,
;                                 :fg-perc-2p 0.528, :fg-perc-3p 0.406, :orb-perc 1.9, :drb-perc 7.8,
;                                 :ast-perc-2p 0.70, :ast-perc-3p 0.95, :ast-perc 10.1, :tov-pct 10.3, :ftr 0.129, :ft-perc 0.82, 
;                                :obpm 0.0, :dbpm 0.9}
;                            :sf {:name "Michael Porter Jr."
;                                 :usage-rate 20.3, :shot-dist-2p 0.463, :shot-dist-3p 0.537,
;                                 :fg-perc-2p 0.596, :fg-perc-3p 0.397, :orb-perc 5.8, :drb-perc 16.6,
;                                 :ast-perc-2p 0.68, :ast-perc-3p 0.90, :ast-perc 6.9, :tov-pct 8.3, :ftr 0.140, :ft-perc 0.79, 
;                                 :obpm 2, :dbpm -0.7}
;                            :pf {:name "Aaron Gordon"
 ;                                :usage-rate 17.8, :shot-dist-2p 0.887, :shot-dist-3p 0.113,
;                                 :fg-perc-2p 0.612, :fg-perc-3p 0.290, :orb-perc 8.4, :drb-perc 15.0,
;                                 :ast-perc-2p 0.65, :ast-perc-3p 0.98, :ast-perc 14.5, :tov-pct 11.2, :ftr 0.334, :ft-perc 0.72, 
;                                 :obpm 1.6, :dbpm 0.4}
;                            :c  {:name "Nikola Jokic"
;                                 :usage-rate 29.8, :shot-dist-2p 0.826, :shot-dist-3p 0.174,
;                                 :fg-perc-2p 0.643, :fg-perc-3p 0.359, :orb-perc 10.9, :drb-perc 31.8,
;                                 :ast-perc-2p 0.55, :ast-perc-3p 0.85, :ast-perc 42.3, :tov-pct 13.5, :ftr 0.392, :ft-perc 0.78, 
;                                 :obpm 9.2, :dbpm 4.1}}))
;   :bench [(with-initial-box-score
;             {:name "Reggie Jackson"
;              :usage-rate 18.1, :shot-dist-2p 0.583, :shot-dist-3p 0.417,
;              :fg-perc-2p 0.488, :fg-perc-3p 0.359, :orb-perc 1.5, :drb-perc 5.5,
;              :ast-perc-2p 0.55, :ast-perc-3p 0.80, :ast-perc 24.1,
;              :tov-pct 11.9, :ftr 0.155, :ft-perc 0.86, 
;              :obpm -0.2, :dbpm -1.5})
;           (with-initial-box-score
;             {:name "Christian Braun"
;              :usage-rate 12.5, :shot-dist-2p 0.701, :shot-dist-3p 0.299,
;              :fg-perc-2p 0.547, :fg-perc-3p 0.384, :orb-perc 4.4, :drb-perc 8.9,
;              :ast-perc-2p 0.60, :ast-perc-3p 0.95, :ast-perc 9.2,
;              :tov-pct 10.1, :ftr 0.231, :ft-perc 0.76, 
;              :obpm -0.4, :dbpm 0.9})]})

;; Warriors
;(def team-b
 ; {:name "Golden State Warriors"
;   :on-court (into {} (map (fn [[pos p]] [pos (with-initial-box-score p)])
;                           {:pg {:name "Stephen Curry"
;                                 :usage-rate 29.2, :shot-dist-2p 0.463, :shot-dist-3p 0.537,
;                                 :fg-perc-2p 0.518, :fg-perc-3p 0.408, :orb-perc 2.0, :drb-perc 12.1,
;                                 :ast-perc-2p 0.50, :ast-perc-3p 0.75, :ast-perc 25.4, :tov-pct 12.1, :ftr 0.222, :ft-perc 0.93 
;                                 :obpm 6.4, :dbpm -1.1}
;                            :sf {:name "Andrew Wiggins"
;                                 :usage-rate 18.2, :shot-dist-2p 0.662, :shot-dist-3p 0.338,
;                                 :fg-perc-2p 0.505, :fg-perc-3p 0.358, :orb-perc 5.2, :drb-perc 11.2,
 ;                                :ast-perc-2p 0.60, :ast-perc-3p 0.97, :ast-perc 8.4, :tov-pct 9.8, :ftr 0.201, :ft-perc 0.82, 
;                                 :obpm -1.6, :dbpm -0.1}
;                            :sg {:name "Klay Thompson"
;                                 :usage-rate 22.9, :shot-dist-2p 0.383, :shot-dist-3p 0.617,
;                                 :fg-perc-2p 0.493, :fg-perc-3p 0.387, :orb-perc 1.7, :drb-perc 8.7,
 ;                                :ast-perc-2p 0.72, :ast-perc-3p 0.96, :ast-perc 13.1, :tov-pct 8.3, :ftr 0.113, :ft-perc 0.91, 
  ;                               :obpm 0.5, :dbpm -2.2}
   ;                         :pf {:name "Draymond Green"
    ;                             :usage-rate 13.5, :shot-dist-2p 0.685, :shot-dist-3p 0.315,
     ;                            :fg-perc-2p 0.547, :fg-perc-3p 0.395, :orb-perc 3.7, :drb-perc 21.0,
      ;                           :ast-perc-2p 0.65, :ast-perc-3p 0.99, :ast-perc 28.6, :tov-pct 18.2, :ftr 0.244, :ft-perc 0.71, 
       ;                          :obpm 1.2, :dbpm 3.8}
        ;                    :c  {:name "Kevon Looney"
         ;                        :usage-rate 10.1, :shot-dist-2p 0.992, :shot-dist-3p 0.008,
          ;                       :fg-perc-2p 0.598, :fg-perc-3p 0.000, :orb-perc 14.1, :drb-perc 21.4,
           ;                      :ast-perc-2p 0.80, :ast-perc-3p 1.0, :ast-perc 12.5, :tov-pct 13.4, :ftr 0.208, :ft-perc 0.67, 
            ;                     :obpm -0.6, :dbpm 0.0}}))
;   :bench [(with-initial-box-score
 ;            {:name "Chris Paul"
  ;            :usage-rate 16.5, :shot-dist-2p 0.672, :shot-dist-3p 0.328,
   ;           :fg-perc-2p 0.481, :fg-perc-3p 0.371, :orb-perc 1.9, :drb-perc 12.5,
    ;          :ast-perc-2p 0.50, :ast-perc-3p 0.85, :ast-perc 36.4,
     ;         :tov-pct 13.9, :ftr 0.188, :ft-perc 0.82, 
      ;        :obpm 1.9, :dbpm 1.0})
       ;    (with-initial-box-score
        ;     {:name "Jonathan Kuminga"
         ;     :usage-rate 24.1, :shot-dist-2p 0.871, :shot-dist-3p 0.129,
          ;    :fg-perc-2p 0.575, :fg-perc-3p 0.321, :orb-perc 5.5, :drb-perc 13.5,
           ;   :ast-perc-2p 0.35, :ast-perc-3p 0.90, :ast-perc 12.3,
            ;  :tov-pct 12.3, :ftr 0.360, :ft-perc 0.74, 
             ; :obpm 0.1, :dbpm -0.9})]})

;; Select finisher function based on their usage rate
(defn select-finisher
  "Selects players randomly based on their usage rate"
  [players]
  (let [total-usage (reduce + (map :usage-rate players))]
    (loop [p (rand total-usage)
           [current-player & rest-players] players]
      (if (< p (:usage-rate current-player))
        current-player
        (recur (- p (:usage-rate current-player)) rest-players)))))

;; Function to fetermine a possesion's outcome and pick between turnovers, shots or free-throws
(defn decide-possession-outcome [finisher]
  (let [r (rand)
        tov-pct (/ (:tov-pct finisher) 100)
        ftr (:ftr finisher)]
    (cond
      (< r tov-pct) :turnover
      (< r (+ tov-pct (* (- 1 tov-pct) (/ ftr 2)))) :foul
      :else :shot)))

;; Function that add a penalty to shot percentages based on player's fatigue
(defn apply-fatigue [player shot-percentage]
  (let [stamina (:stamina player)
        fatigue-penalty (cond
                          (> stamina 75) 1.0
                          (> stamina 50) 0.85
                          (> stamina 25) 0.7
                          :else          0.55)]
    (* shot-percentage fatigue-penalty)))

(defn apply-defense
  "Modifies a players shot percentage based on the defender who's guarding them"
  [player defender shot-percentage] 
  (let [bpm-difference (- (:obpm player) (:dbpm defender))] 
   (* shot-percentage (+ 1 (/ bpm-difference 100)))))

;;Shot simualtion function
(defn simulate-shot
  "Simulates shot. a Chooses whether the shot is 2p or 3p based on their tendencies. Hit or miss based on shot percentage"
  [player defender]
  (let [shot-type (if (< (rand) (:shot-dist-3p player)) :3p :2p)
        shot-made? (if (= shot-type :3p)
                     (< (rand) (apply-defense player defender (apply-fatigue player (:fg-perc-3p player))))
                     (< (rand) (apply-defense player defender (apply-fatigue player (:fg-perc-2p player)))))] 
    {:shot-type shot-type :shot-made? shot-made?}))

;; Rebound simulation function
(defn simulate-rebound
  "Simulates a rebound, selecting a player from all players on court"
  [off-team def-team]
  (let [off-players (vals off-team)
        def-players (vals def-team)
        all-players (concat off-players def-players)
        rebound-chances (map (fn [p] (if (some #{p} off-players) (:orb-perc p) (:drb-perc p))) all-players)
        total-rebound-chance (reduce + rebound-chances)]
    (loop [r (rand total-rebound-chance)
           [player & remaining] all-players
           [chance & chances] rebound-chances]
      (if (< r chance)
        player
        (recur (- r chance) remaining chances)))))

;; Assist simulation function
(defn simulate-assist
  "Simulates a potential assist on a made shot using the hybrid logic."
  [shooter shot-type team]
  (let [assist-chance (if (= shot-type :3p)
                        (:ast-perc-3p shooter)
                        (:ast-perc-2p shooter))]

    (when (< (rand) assist-chance)

      (let [shooter-pos (some (fn [[pos p]] (when (= p shooter) pos)) team)
            potential-assisters (vals (dissoc team shooter-pos))
            total-assist-weight (reduce + (map :ast-perc potential-assisters))]

        (when (pos? total-assist-weight)
          (loop [r (rand total-assist-weight)
                 [player & remaining] potential-assisters]
            (let [weight (:ast-perc player)]
              (if (< r weight)
                player
                (recur (- r weight) remaining)))))))))

;; Initial game state
(defn initial-game-state
  []
  {:game-clock 720 ; 12 * 60 seconds
   :shot-clock 24
   :quarter 1
   :offense :team-a
   :defense :team-b
   :score {:team-a 0 :team-b 0}
   :teams (run-draft (load-players player-data-path) 160)})

;; Possesion time simulation - fix later, add more complex logic to the game
(defn simulate-possession-time
  "Returns a random amount of time a possession might take."
  [shot-clock]
  (+ 8 (rand-int (- shot-clock 8))))


;; Function to find a playerss position in the lineup
(defn find-player-pos [team player]
  (some (fn [[pos p]] (when (= p player) pos)) team))


;; Function that decreases stamina for every possesion played
(defn update-stamina [on-court-players]
  (into {} (map (fn [[pos p]]
                  [pos (cond (> (p :stamina) 0) (update p :stamina - 0.66)
                             :else p)])
                on-court-players)))

;; Function that increases stamina for players sitting on the bench
(defn rest-players [bench]
  (into [] (map (fn [p]
                  (cond (< (:stamina p) 100) (update p :stamina + 1)
                        :else p))
                bench)))

;; Funcction that resolves a shot being taken, wether it's a hit or a miss, and if there are assists or rebounds
(defn resolve-shot
  "Resovles the aftermath of a shot being taken, wether it's a hit, a miss and if there are assists and rebounds"
  [game-state finisher]
  (let [off-team-id (:offense game-state)
        def-team-id (:defense game-state)
        off-team (get-in game-state [:teams off-team-id :on-court])
        def-team (get-in game-state [:teams def-team-id :on-court])
        finisher-pos (find-player-pos off-team finisher)
        defender (get-in game-state [:teams def-team-id :on-court finisher-pos])
        shot-result (simulate-shot finisher defender)
        possession-time (simulate-possession-time (:shot-clock game-state))
        is-3p? (= :3p (:shot-type shot-result))]


    (println "-------------------------")
    (println (:name (get-in game-state [:teams off-team-id])) "have the ball.")
    (println (:name finisher) "takes a" (name (:shot-type shot-result)) "pointer...")
    (if (:shot-made? shot-result)
      (let [assister (simulate-assist finisher (:shot-type shot-result) off-team)
            points (if (= :3p (:shot-type shot-result)) 3 2)
            assister-pos (when assister (find-player-pos off-team assister))]
        (if assister
          (println "It's good! Assist by" (:name assister))
          (println "It's good! Unassisted."))

        (-> game-state
            (update :game-clock - possession-time)
            (assoc :shot-clock 24)
            (update-in [:teams :team-a :on-court] update-stamina)
            (update-in [:teams :team-b :on-court] update-stamina)
            (update-in [:teams :team-a :bench] rest-players)
            (update-in [:teams :team-b :bench] rest-players)
            (update-in [:score off-team-id] + points)
            (update-in [:teams off-team-id :on-court finisher-pos :box-score :pts] + points)
            (update-in [:teams off-team-id :on-court finisher-pos :box-score :fgm] inc)
            (update-in [:teams off-team-id :on-court finisher-pos :box-score :fga] inc)
            (update-in [:teams off-team-id :on-court finisher-pos :box-score :3pm] (if is-3p? inc identity))
            (update-in [:teams off-team-id :on-court finisher-pos :box-score :3pa] (if is-3p? inc identity))
            (cond-> assister-pos (update-in [:teams off-team-id :on-court assister-pos :box-score :ast] inc))            (assoc :offense def-team-id)
            (assoc :defense off-team-id)))

      (let [rebounder (simulate-rebound off-team def-team)]
        (println "It's a miss!")
        (println "Rebound by:" (:name rebounder))

        (let [offensive-rebound? (some #(= rebounder %) (vals off-team))]
          (-> game-state
              (update :game-clock - possession-time)
              (update-in [:teams :team-a :on-court] update-stamina)
              (update-in [:teams :team-b :on-court] update-stamina)
              (update-in [:teams :team-a :bench] rest-players)
              (update-in [:teams :team-b :bench] rest-players)
              (update-in [:teams off-team-id :on-court finisher-pos :box-score :fga] inc)
              (update-in [:teams off-team-id :on-court finisher-pos :box-score :3pa] (if is-3p? inc identity))
              (update-in [:teams (if offensive-rebound? off-team-id def-team-id) :on-court (find-player-pos (if offensive-rebound? off-team def-team) rebounder) :box-score :reb] inc)
              (assoc :shot-clock (if offensive-rebound?
                                   14
                                   24))
              (assoc :offense (if offensive-rebound?
                                off-team-id
                                def-team-id))
              (assoc :defense (if offensive-rebound?
                                def-team-id
                                off-team-id))))))))

;; Function that resolves a turnover being made
(defn resolve-turnover
  "Resolves a trunover by updating the box-score for a player and updating the game state and changing the possesion"
  [game-state finisher]
  (let [off-team-id (:offense game-state)
        def-team-id (:defense game-state)
        off-team (get-in game-state [:teams off-team-id :on-court])
        finisher-pos (find-player-pos off-team finisher)
        possession-time (simulate-possession-time (:shot-clock game-state))]
    (println "-------------------------")
    (println (:name (get-in game-state [:teams off-team-id])) "have the ball.")
    (println "It's a trunover!")
    (println (:name finisher) " has lost the ball!")
    (-> game-state
        (update :game-clock - possession-time)
        (update-in [:teams :team-a :on-court] update-stamina)
        (update-in [:teams :team-b :on-court] update-stamina)
        (update-in [:teams :team-a :bench] rest-players)
        (update-in [:teams :team-b :bench] rest-players)
        (assoc :offense def-team-id)
        (assoc :defense off-team-id)
        (update-in [:teams off-team-id :on-court finisher-pos :box-score :tov] inc))))

;; Function that resolves free throws being taken - update the logic to include and-1s
(defn resolve-foul
  "Resolves free throws based on players free throw percentage"
  [game-state finisher]
  (let [off-team-id (:offense game-state)
        def-team-id (:defense game-state)
        off-team (get-in game-state [:teams off-team-id :on-court])
        finisher-pos (find-player-pos off-team finisher)
        possession-time (simulate-possession-time (:shot-clock game-state))
        ft-1 (< (rand) (apply-fatigue finisher (get-in game-state [:teams off-team-id :on-court finisher-pos :ft-perc])))
        ft-2 (< (rand) (apply-fatigue finisher (get-in game-state [:teams off-team-id :on-court finisher-pos :ft-perc])))
        made-shots (cond (and ft-1 ft-2) 2
                         (or ft-1 ft-2) 1
                         :else 0)]
    (println "-------------------------")
    (println (:name (get-in game-state [:teams off-team-id])) "have the ball.")
    (println "It's a foul!")
    (println (finisher :name) " takes free throws")
    (println "He's made " made-shots " of 2 free-throws!")
    (-> game-state
        (update :game-clock - possession-time)
        (update-in [:teams :team-a :on-court] update-stamina)
        (update-in [:teams :team-b :on-court] update-stamina)
        (update-in [:teams :team-a :bench] rest-players)
        (update-in [:teams :team-b :bench] rest-players)
        (update-in [:score off-team-id] + made-shots)
        (assoc :offense def-team-id)
        (assoc :defense off-team-id)
        (update-in [:teams off-team-id :on-court finisher-pos :box-score :pts] + made-shots)
        (update-in [:teams off-team-id :on-court finisher-pos :box-score :ftm] + made-shots)
        (update-in [:teams off-team-id :on-court finisher-pos :box-score :fta] + 2))))

;; Run possesion function - simulate a possesion and update game state
(defn run-possession
  "Runs a single possession and returns the new game state."
  [game-state]
  (let [off-team-id (:offense game-state)
        finisher (select-finisher (vals (get-in game-state [:teams off-team-id :on-court])))
        outcome (decide-possession-outcome finisher)]
    
    (cond (= outcome :shot) (resolve-shot game-state finisher)
          (= outcome :turnover) (resolve-turnover game-state finisher)
          :else (resolve-foul game-state finisher))))

;; Function to print the box score at the end of the game
(defn print-box-score [game-state]
  (doseq [team-id [:team-a :team-b]]
    (let [team (get-in game-state [:teams team-id])
          team-fga (reduce + (map (comp :fga :box-score) (concat (vals (:on-court team)) (:bench team))))
          team-fgm (reduce + (map (comp :fgm :box-score) (concat (vals (:on-court team)) (:bench team))))
          team-3pa (reduce + (map (comp :3pa :box-score) (concat (vals (:on-court team)) (:bench team))))
          team-3pm (reduce + (map (comp :3pm :box-score) (concat (vals (:on-court team)) (:bench team))))
          team-fta (reduce + (map (comp :fta :box-score) (concat (vals (:on-court team)) (:bench team))))
          team-ftm (reduce + (map (comp :ftm :box-score) (concat (vals (:on-court team)) (:bench team))))
          team-fg-pct (if (> team-fga 0) (float (* 100 (/ team-fgm team-fga))) 0)
          team-3p-pct (if (> team-3pa 0) (float (* 100 (/ team-3pm team-3pa))) 0)
          team-ft-pct (if (> team-fta 0) (float (* 100 (/ team-ftm team-fta))) 0)]
      (println "\n---" (:name team) "---")
      (println "FG : " team-fgm "-" team-fga)
      (println "FG % : " (format "%.2f" team-fg-pct) "%")
      (println "3p : " team-3pm "-" team-3pa)
      (println "3p % :" (format "%.2f " team-3p-pct) "%")
      (println "FT: " team-ftm "-" team-fta)
      (println "FT%: " team-ft-pct)
      (println (format "%-25s %-5s %-5s %-5s %-5s %-10s %-5s %-5s %-5s" "PLAYER" "PTS" "REB" "AST" "FG" "3P" "FT" "TO" "Stamina"))

      (doseq [player (concat (vals (:on-court team)) (:bench team))]
        (let [bs (:box-score player)]
          (println (format "%-25s %-5s %-5s %-5s %s-%-5s %s-%-5s %s-%-5s %-5s %-5s"
                           (:name player)
                           (:pts bs)
                           (:reb bs)
                           (:ast bs)
                           (:fgm bs)
                           (:fga bs)
                           (:3pm bs)
                           (:3pa bs)
                           (:ftm bs)
                           (:fta bs)
                           (:tov bs)
                           (:stamina player))))))))

;; Substitution function to swap players in from the bench
(defn substitution
  [team player-in player-out]
  (let [pos-out (find-player-pos (:on-court team) player-out)]
    (println "A substitution has been made!")
    (println (:name player-out) " is going out for " (:name player-in) "in position " pos-out)
    (-> team (assoc-in [:on-court pos-out] player-in)
        (assoc :bench (conj (filter (fn [p] (not= p player-in)) (:bench team)) player-out)))))

;; Function that queries the user for substitution inputs and performs them
(defn resolve-sub [team] 
  (loop [subbed-team team]
    (println "Would you like to make subtitutions for " (:name team) " ? (y/n)")
    (if (= (read-line) "y")
      (do
        (println "Which player would you like to take out? (pg, sg, sf, pf or c)")
        (let [out (read-line)
              player-out (get-in subbed-team [:on-court (keyword out)])]
          (println "You're subbing out " (:name player-out))
          (println "Which player would ypu like to sub in? (0 or 1)")
          (doseq [[index player] (map-indexed vector (:bench subbed-team))]
            (println index " " (:name player)))
          (let [in (Integer/parseInt (read-line))
                player-in (nth (:bench subbed-team) in)]
            (recur (substitution subbed-team player-in player-out)))))
      subbed-team)))


;; Function to recur possesions until the time runs out
(defn run-quarter [initial-state]
  (loop [game-state initial-state]
    (if (<= (:game-clock game-state) 0)
      (do
        (println "\nEnd of quarter " (:quarter game-state))
        (println "\n---SCORE---")
        (println (get-in game-state [:teams :team-a :name]) ":" (get-in game-state [:score :team-a]))
        (println (get-in game-state [:teams :team-b :name]) ":" (get-in game-state [:score :team-b]))
        (print-box-score game-state)
        (Thread/sleep 3000)
        (let [team-a (resolve-sub (get-in game-state [:teams :team-a]))
              team-b (resolve-sub (get-in game-state [:teams :team-b]))]
          (-> game-state
              (assoc-in [:teams :team-a] team-a)
              (assoc-in [:teams :team-b] team-b) 
              (update :quarter inc)
              (assoc :game-clock 720))))
      (recur (run-possession game-state)))))

;; Run game function that goes through the quarters
(defn run-game [initial-state]
  (loop [game-state initial-state]
    (if (> (:quarter game-state) 4)
      game-state
      (recur (run-quarter game-state)))))


;; Main function that simulates the game
(defn -main
  "Simulates a full game and prints the final box score."
  [& args]
  (println "Simulating game...")
  (let [final-state (run-game (initial-game-state))]
    (println "\n--- FINAL SCORE ---")
    (println (get-in final-state [:teams :team-a :name]) ":" (get-in final-state [:score :team-a]))
    (println (get-in final-state [:teams :team-a :name]) ":" (get-in final-state [:score :team-b]))
    (print-box-score final-state)))

(-main)