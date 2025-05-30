/**
 * Jobs Plugin for Bukkit
 * Copyright (C) 2011 Zak Ford <zak.j.ford@gmail.com>
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.gamingmesh.jobs.container;

import java.util.HashMap;
import java.util.Map;

import com.gamingmesh.jobs.Jobs;

import net.Zrips.CMILib.Container.CMINumber;
import net.Zrips.CMILib.Time.CMITimeManager;

public class JobProgression {
    private Job job;
    private JobsPlayer jPlayer;
    private double experience;
    private double lastExperience = 0;
    private int level;
    private transient int maxExperience = -1;
    private long leftOn = 0;

    public JobProgression(Job job, JobsPlayer jPlayer, int level, double experience) {
        this.job = job;
        this.jPlayer = jPlayer;
        this.experience = experience;
        this.level = level;

        JobsTop.updateTops(job, jPlayer, level, experience);
    }

    /**
     * Can the job level up?
     * @return true if the job can level up
     * @return false if the job cannot
     */
    public boolean canLevelUp() {
        return experience >= maxExperience;
    }

    /**
     * Can the job level down?
     * @return true if the job can level up
     * @return false if the job cannot
     */
    public boolean canLevelDown() {
        return experience < 0;
    }

    /**
     * Return the job
     * @return the job
     */
    public Job getJob() {
        return job;
    }

    /**
     * Set the job
     * @param job - the new job to be set
     */
    public void setJob(Job job) {
//		synchronized (jPlayer.saveLock) {
        jPlayer.setSaved(false);
        this.job = job;
        reloadMaxExperienceAndCheckLevelUp();
//		}
    }

    /**
     * Get the experience in this job
     * @return the experiece in this job
     */
    public double getExperience() {
        return experience;
    }

    /**
     * Adds experience for this job
     * @param experience - the experience in this job
     * @return - job level up
     */
    public boolean addExperience(double experience) {
        jPlayer.setSaved(false);
        this.experience += experience;
        lastExperience = getLastExperience() + experience;
        return checkLevelUp();
    }

    /**
     * Sets experience for this job
     * @param experience - the experience in this job
     * @return - job level up
     */
    public boolean setExperience(double experience) {
        jPlayer.setSaved(false);
        this.experience = experience;
        return checkLevelUp();
    }

    /**
     * Takes experience from this job
     * @param experience - the experience in this job
     * @return - job level up
     */
    public boolean takeExperience(double experience) {
        jPlayer.setSaved(false);
        this.experience -= experience;
        lastExperience = getLastExperience() + experience;
        return checkLevelUp();
    }

    /**
     * Get the maximum experience for this level
     * @return the experience needed to level up
     */
    public int getMaxExperience() {
        return maxExperience;
    }

    /**
     * Get the current level of this job
     * @return the level of this job
     */
    public int getLevel() {
        return level;
    }

    /**
     * Get the current level of this job in formatted way
     * @return the level of this job
     */
    public String getLevelFormatted() {
        if (Jobs.getGCManager().RomanNumbers)
            return CMINumber.toRoman(level);
        return String.valueOf(level);
    }

    /**
     * Sets the level of this job progression
     * 
     * @param level the new level for this job
     * @return true if this progression can level up
     */
    public boolean setLevel(int level) {
        jPlayer.setSaved(false);
        this.level = level;
        return reloadMaxExperienceAndCheckLevelUp();
    }

    /**
     * Reloads max experience
     */
    public void reloadMaxExperience() {
        Map<String, Double> param = new HashMap<>();
        param.put("joblevel", (double) level);
        param.put("numjobs", (double) jPlayer.getJobProgression().size());
        maxExperience = (int) job.getMaxExp(param);
    }

    public int getMaxExperience(int level) {
        Map<String, Double> param = new HashMap<>();
        param.put("joblevel", (double) level);
        param.put("numjobs", (double) jPlayer.getJobProgression().size());
        return (int) job.getMaxExp(param);
    }

    /**
     * Performs a level up
     * @returns if level up was performed
     */
    private boolean checkLevelUp() {
        if (level == 1 && experience < 0)
            experience = 0;

        if (experience < 0)
            return checkLevelDown();

        boolean ret = false;
        while (canLevelUp()) {
            // Don't level up at max level
            if (job.getMaxLevel() > 0 && level >= jPlayer.getMaxJobLevelAllowed(job))
                break;

            level++;
            experience -= maxExperience;

            ret = true;
            reloadMaxExperience();
            jPlayer.reloadLimits();
        }

        // At max level
        if (experience > maxExperience)
            experience = maxExperience;

        JobsTop.updateTops(job, jPlayer, level, experience);

        return ret;
    }

    /**
     * Performs a level down
     * @returns if level down was performed
     */
    private boolean checkLevelDown() {
        boolean ret = false;
        while (canLevelDown()) {
            if (
            // Don't level down at 1
            level <= 1 ||
                !Jobs.getGCManager().AllowDelevel) {
                experience = 0;

                break;
            }

            level--;
            experience += getMaxExperience(level);

            ret = true;
            reloadMaxExperience();

            jPlayer.reloadLimits();
        }

        JobsTop.updateTops(job, jPlayer, level, experience);

        return ret;
    }

    /**
     * Reloads max experience and checks for level up
     * Do this whenever job or level changes
     * @return if leveled up
     */
    private boolean reloadMaxExperienceAndCheckLevelUp() {
        reloadMaxExperience();
        return checkLevelUp();
    }

    public Long getLeftOn() {
        return leftOn;
    }

    public JobProgression setLeftOn(Long leftOn) {
        this.leftOn = leftOn;
        return this;
    }

    public boolean canRejoin() {
        if (leftOn == 0 || leftOn + job.getRejoinCd() < System.currentTimeMillis())
            return true;

        org.bukkit.entity.Player player = jPlayer != null ? jPlayer.getPlayer() : null;
        return player != null && player.hasPermission("jobs.rejoinbypass");
    }

    public String getRejoinTimeMessage() {
        return leftOn == 0 ? "" : CMITimeManager.to24hourShort(leftOn + job.getRejoinCd() - System.currentTimeMillis());
    }

    public double getLastExperience() {
        return lastExperience;
    }

    public void setLastExperience(double lastExperience) {
        this.lastExperience = lastExperience;
    }

}
