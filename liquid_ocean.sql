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

--
-- Dumping data for table `devices`
--

INSERT INTO `devices` (`id`, `uuid`, `name`, `paint_qty`, `type`, `xp`, `wt`, `st`, `tp`, `oi`, `oo`, `banned`, `pincode`) VALUES
(1, '4e1a82b9-10e7-4576-be17-012ccb073123', '', 5, 0, 0, 0, 0, 0, 0, 0, 0, ''),
(2, '23098446-80b3-4fda-9993-948edea464d4', '', 5, 0, 0, 0, 0, 0, 0, 0, 0, ''),
(3, '23098446-80b3-4fda-9993-948edea464d4', '', 5, 0, 0, 0, 0, 0, 0, 0, 0, ''),
(4, '5567109b-bb4e-4424-a971-f6b5e8c0f3c1', '', 5, 0, 0, 0, 0, 0, 0, 0, 0, ''),
(5, '5fe04d1c-f6f9-43a6-8065-9c47c5a571a3', '', 5, 0, 0, 0, 0, 0, 0, 0, 0, '');

-- --------------------------------------------------------

--
-- Table structure for table `devices_ips`
--

CREATE TABLE `devices_ips` (
  `id` int NOT NULL,
  `device_id` int NOT NULL,
  `ip_id` int NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Dumping data for table `devices_ips`
--

INSERT INTO `devices_ips` (`id`, `device_id`, `ip_id`) VALUES
(1, 1, 1),
(2, 2, 1),
(3, 4, 1),
(4, 5, 1);

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

--
-- Dumping data for table `ips`
--

INSERT INTO `ips` (`id`, `address`, `banned`) VALUES
(1, '74.138.80.212', 0);

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

--
-- Dumping data for table `pixels`
--

INSERT INTO `pixels` (`id`, `color`, `device_id`, `realm_id`) VALUES
(522751, -11282419, 1, 1),
(522753, -1523179, 5, 1),
(522755, -11282419, 1, 1),
(522759, -11282419, 1, 1),
(522761, -11282419, 1, 1),
(523768, -4815797, 5, 1),
(523769, -4815797, 5, 1),
(523770, -4815797, 5, 1),
(523783, -11282419, 1, 1),
(523784, -11282419, 1, 1),
(523785, -11282419, 1, 1),
(524792, -4815797, 5, 1),
(524793, -14008801, 1, 1),
(524794, -14008801, 1, 1),
(524799, -1, 1, 1),
(524801, -1, 1, 1),
(524803, -1523179, 5, 1),
(524808, -10733036, 5, 1),
(525816, -4815797, 5, 1),
(525817, -14008801, 1, 1),
(525818, -14008801, 1, 1),
(525832, -10733036, 5, 1),
(526841, -14008801, 1, 1),
(526847, -11282419, 1, 1),
(526849, -1523179, 5, 1),
(526856, -10733036, 5, 1),
(528888, -8075935, 1, 1),
(529912, -8075935, 1, 1),
(529913, -8075935, 1, 1);

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

--
-- Dumping data for table `pixel_history`
--

INSERT INTO `pixel_history` (`id`, `timestamp`, `color`, `device_id`, `pixel_id`) VALUES
(1, 1738881847, -1, 1, 524799),
(2, 1738881864, -1, 1, 524801),
(3, 1738882308, -11282419, 1, 522751),
(4, 1738886007, -1523179, 5, 522753),
(5, 1738886071, -11282419, 1, 526847),
(6, 1738886215, -1523179, 5, 526849),
(7, 1738886938, -11282419, 1, 522755),
(8, 1738887041, -1523179, 5, 524803),
(9, 1738887278, -10733036, 5, 524808),
(10, 1738887278, -10733036, 5, 525832),
(11, 1738887278, -10733036, 5, 526856),
(12, 1738887323, -11282419, 1, 522761),
(13, 1738887324, -11282419, 1, 523785),
(14, 1738887324, -11282419, 1, 523784),
(15, 1738887324, -11282419, 1, 523783),
(16, 1738887324, -11282419, 1, 522759),
(17, 1738901233, -14008801, 1, 524794),
(18, 1738901233, -14008801, 1, 525818),
(19, 1738901233, -14008801, 1, 525817),
(20, 1738901233, -14008801, 1, 526841),
(21, 1738901233, -14008801, 1, 524793),
(22, 1738901246, -4815797, 5, 523770),
(23, 1738901246, -4815797, 5, 523769),
(24, 1738901246, -4815797, 5, 523768),
(25, 1738901246, -4815797, 5, 524792),
(26, 1738901246, -4815797, 5, 525816),
(27, 1738901383, -8075935, 1, 529913),
(28, 1738901383, -8075935, 1, 529912),
(29, 1738901394, -8075935, 1, 528888);

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

--
-- Dumping data for table `realms`
--

INSERT INTO `realms` (`id`, `name`, `w`, `h`) VALUES
(1, 'Test Canvas 1', 512, 512);

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
-- Indexes for dumped tables
--

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
-- AUTO_INCREMENT for table `pixels`
--
ALTER TABLE `pixels`
  MODIFY `id` int NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=529914;

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
