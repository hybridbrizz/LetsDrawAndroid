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
-- Database: `liquid_ocean_servers`
--

-- --------------------------------------------------------

--
-- Table structure for table `servers`
--

CREATE TABLE `servers` (
  `id` int NOT NULL,
  `name` text NOT NULL,
  `access_key` text NOT NULL,
  `admin_key` text NOT NULL,
  `base_url` text NOT NULL,
  `api_port` int NOT NULL,
  `socket_port` int NOT NULL,
  `queue_port` int NOT NULL,
  `alt_port` int NOT NULL,
  `icon_url` text NOT NULL,
  `icon_link` text NOT NULL,
  `color` int NOT NULL,
  `banner_text` text NOT NULL,
  `show_banner` int NOT NULL,
  `pixel_interval` int NOT NULL,
  `max_pixels` int NOT NULL,
  `pixels_amt` int NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Dumping data for table `servers`
--

INSERT INTO `servers` (`id`, `name`, `access_key`, `admin_key`, `base_url`, `icon_url`, `icon_link`, `color`, `banner_text`, `show_banner`, `pixel_interval`, `max_pixels`, `pixels_amt`) VALUES
(1, 'Test Canvas', 'TEST1', '35QSVZS1', 'https://matrixwarez.com', '', '', 0, '', 0, 60, 5, 1);

--
-- Indexes for dumped tables
--

--
-- Indexes for table `servers`
--
ALTER TABLE `servers`
  ADD PRIMARY KEY (`id`);

--
-- AUTO_INCREMENT for dumped tables
--

--
-- AUTO_INCREMENT for table `servers`
--
ALTER TABLE `servers`
  MODIFY `id` int NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=2;
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
