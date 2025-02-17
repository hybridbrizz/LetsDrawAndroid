-- phpMyAdmin SQL Dump
-- version 5.2.1
-- https://www.phpmyadmin.net/
--
-- Host: localhost
-- Generation Time: Feb 07, 2025 at 04:15 AM
-- Server version: 8.0.41-0ubuntu0.22.04.1
-- PHP Version: 8.1.2-1ubuntu2.20

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- Database: `liquid_ocean`
--

-- --------------------------------------------------------

--
-- Table structure for table `devices`
--

CREATE TABLE `devices` (
  `id` int NOT NULL,
  `uuid` text NOT NULL,
  `name` text NOT NULL,
  `paint_qty` int NOT NULL,
  `type` int NOT NULL,
  `xp` int NOT NULL,
  `wt` int NOT NULL,
  `st` int NOT NULL,
  `tp` int NOT NULL,
  `oi` int NOT NULL,
  `oo` int NOT NULL,
  `banned` int NOT NULL,
  `pincode` text NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- --------------------------------------------------------

--
-- Table structure for table `devices_ips`
--

CREATE TABLE `devices_ips` (
  `id` int NOT NULL,
  `device_id` int NOT NULL,
  `ip_id` int NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- --------------------------------------------------------

--
-- Table structure for table `global_vars`
--

CREATE TABLE `global_vars` (
  `id` int NOT NULL,
  `last_paint_time` int NOT NULL,
  `last_top_contributors_time` int NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- --------------------------------------------------------

--
-- Table structure for table `ips`
--

CREATE TABLE `ips` (
  `id` int NOT NULL,
  `address` text NOT NULL,
  `banned` int NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- --------------------------------------------------------

--
-- Table structure for table `paint_events`
--

CREATE TABLE `paint_events` (
  `id` int NOT NULL,
  `timestamp` int NOT NULL,
  `num_devices` int NOT NULL,
  `amount` int NOT NULL,
  `total_amount` int NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- --------------------------------------------------------

--
-- Table structure for table `pixels`
--

CREATE TABLE `pixels` (
  `id` int NOT NULL,
  `color` int NOT NULL,
  `device_id` int NOT NULL,
  `realm_id` int NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- --------------------------------------------------------

--
-- Table structure for table `pixel_history`
--

CREATE TABLE `pixel_history` (
  `id` int NOT NULL,
  `timestamp` int NOT NULL,
  `color` int NOT NULL,
  `device_id` int NOT NULL,
  `pixel_id` int NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- --------------------------------------------------------

--
-- Table structure for table `realms`
--

CREATE TABLE `realms` (
  `id` int NOT NULL,
  `name` text NOT NULL,
  `w` int NOT NULL,
  `h` int NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- --------------------------------------------------------

--
-- Table structure for table `top_contributors`
--

CREATE TABLE `top_contributors` (
  `id` int NOT NULL,
  `timestamp` int NOT NULL,
  `device_id` int NOT NULL,
  `amt` int NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Indexes for table `devices`
--
ALTER TABLE `devices`
  ADD PRIMARY KEY (`id`);

--
-- Indexes for table `devices_ips`
--
ALTER TABLE `devices_ips`
  ADD PRIMARY KEY (`id`);

--
-- Indexes for table `global_vars`
--
ALTER TABLE `global_vars`
  ADD PRIMARY KEY (`id`);

--
-- Indexes for table `ips`
--
ALTER TABLE `ips`
  ADD PRIMARY KEY (`id`);

--
-- Indexes for table `paint_events`
--
ALTER TABLE `paint_events`
  ADD PRIMARY KEY (`id`);

--
-- Indexes for table `pixels`
--
ALTER TABLE `pixels`
  ADD PRIMARY KEY (`id`);

--
-- Indexes for table `pixel_history`
--
ALTER TABLE `pixel_history`
  ADD PRIMARY KEY (`id`);

--
-- Indexes for table `realms`
--
ALTER TABLE `realms`
  ADD PRIMARY KEY (`id`);

--
-- Indexes for table `top_contributors`
--
ALTER TABLE `top_contributors`
  ADD PRIMARY KEY (`id`);

--
-- AUTO_INCREMENT for dumped tables
--

--
-- AUTO_INCREMENT for table `devices`
--
ALTER TABLE `devices`
  MODIFY `id` int NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=6;

--
-- AUTO_INCREMENT for table `devices_ips`
--
ALTER TABLE `devices_ips`
  MODIFY `id` int NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=5;

--
-- AUTO_INCREMENT for table `global_vars`
--
ALTER TABLE `global_vars`
  MODIFY `id` int NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `ips`
--
ALTER TABLE `ips`
  MODIFY `id` int NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=2;

--
-- AUTO_INCREMENT for table `paint_events`
--
ALTER TABLE `paint_events`
  MODIFY `id` int NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `pixel_history`
--
ALTER TABLE `pixel_history`
  MODIFY `id` int NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=30;

--
-- AUTO_INCREMENT for table `realms`
--
ALTER TABLE `realms`
  MODIFY `id` int NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=2;

--
-- AUTO_INCREMENT for table `top_contributors`
--
ALTER TABLE `top_contributors`
  MODIFY `id` int NOT NULL AUTO_INCREMENT;
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
