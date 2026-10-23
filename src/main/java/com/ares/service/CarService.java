package com.ares.service;

import com.ares.dto.ShopDtos.CategoryDto;
import com.ares.dto.ShopDtos.ReviewDto;
import com.ares.model.Car;
import com.ares.repository.CarRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class CarService {

    private final CarRepository carRepository;

    @Autowired
    public CarService(CarRepository carRepository) {
        this.carRepository = carRepository;
    }

    @PostConstruct
    public void initSeedData() {
        carRepository.deleteAll();
        List<Car> seedCars = new ArrayList<>();

        // 1. Best Sellers
        seedCars.add(new Car(
            "Huracan STO RC Drifting", "PS 340", 1890000L, 2360000L,
            "/images/cars/bestseller-1.jpg", "BEST_SELLER", "RC_CARS",
            "1:10 Drift Spec", "Khung gầm Carbon & Nhôm 6061",
            "Động cơ Brushless, 60km/h, ESC 60A chống nước",
            "Mô hình RC Lamborghini Huracan STO bản giới hạn, tốc độ cao với hệ thống dẫn động 4 bánh toàn thời gian (4WD), tay cầm 2.4GHz không độ trễ."
        ));

        seedCars.add(new Car(
            "Pagani Huayra R Full Carbon", "Huayra R", 2450000L, 3100000L,
            "/images/cars/bestseller-2.jpg", "BEST_SELLER", "MODEL_CARS",
            "1:18 Collector Scale", "Hợp kim kẽm Diecast cao cấp + Vân Carbon phủ bóng",
            "Mở cánh cửa cánh chim gull-wing, nắp capo trước sau, chi tiết khối động cơ V12",
            "Mô hình tĩnh siêu xe Pagani Huayra R hoàn thiện tinh xảo bậc nhất thế giới, nội thất bọc da thật thu nhỏ, mâm xe cắt CNC sắc nét."
        ));

        seedCars.add(new Car(
            "Ferrari F40 Berlinetta Rosso", "FR F40", 2890000L, 3600000L,
            "/images/cars/bestseller-3.jpg", "BEST_SELLER", "MODEL_CARS",
            "1:18 Collector Scale", "Hợp kim đúc nguyên khối sơn tĩnh điện 4 lớp Rosso Corsa",
            "Mở nắp động cơ sau V8 Twin-Turbo, mở đèn mắt lật pop-up",
            "Huyền thoại siêu xe Ferrari F40 nguyên bản, tái hiện sắc nét từ cánh gió đuôi trứ danh tới khối động cơ V8 tăng áp kép."
        ));

        // 2. RC Highlights
        seedCars.add(new Car(
            "BMW M4 Competition RC Drift", "M4 Drift", 1290000L, 1600000L,
            "/images/cars/rc-1.jpg", "RC_HIGHLIGHTS", "RC_CARS",
            "1:10 Drift Spec", "Khung gia cố sợi thủy tinh, lốp Drift chuyên dụng",
            "Vi sai cầu sau độ khóa (locked diff), góc lái mở rộng 65 độ, đèn pha LED",
            "Chiếc xe RC hoàn hảo cho các tay lái đam mê bộ môn drift mô hình. Dễ kiểm soát góc trượt và phản hồi ga cực kỳ chân thực."
        ));

        seedCars.add(new Car(
            "Nissan Skyline GT-R R34 Nismo", "DP 450", 2150000L, 2700000L,
            "/images/cars/rc-2.jpg", "RC_HIGHLIGHTS", "RC_CARS",
            "1:10 High-Speed Spec", "Hợp kim nhôm T6 & Nhựa ABS siêu bền",
            "Hệ dẫn động đai 2 cầu (belt-drive), tốc độ tối đa 75km/h, pin Lipo 3S 5200mAh",
            "Huyền thoại Godzilla Skyline GT-R R34 trong lớp áo Z-Tune Nismo. Sức mạnh bùng nổ trên mọi cung đường đua tốc độ."
        ));

        seedCars.add(new Car(
            "Porsche 911 GT3 RS Track Spec", "HS 832 GTB", 1690000L, 2100000L,
            "/images/cars/rc-3.jpg", "RC_HIGHLIGHTS", "RC_CARS",
            "1:10 Grip Spec", "Khung gầm carbon mật độ cao, cánh gió khí động học DRS chủ động",
            "Hệ thống con quay hồi chuyển Gyro ổn định lái, motor không chổi than 3650",
            "Mẫu xe RC chinh phục các khúc cua gắt với độ bám đường đỉnh cao, được mô phỏng theo chiến mã Nurburgring của thương hiệu Stuttgart."
        ));

        // 3. Model Highlights
        seedCars.add(new Car(
            "Bugatti Chiron Pur Sport 1:18", "PS 938 BMR", 3200000L, 4000000L,
            "/images/cars/model-1.jpg", "MODEL_HIGHLIGHTS", "MODEL_CARS",
            "1:18 Collector Scale", "Hợp kim kẽm Diecast cao cấp màu sơn đen nhám Matte Black",
            "Mở khoang hành lý phía trước, mở 2 cửa hông, cánh gió sau cố định cỡ lớn",
            "Mô hình Bugatti Chiron Pur Sport mang tông màu đen nhám huyền bí, khoang động cơ quad-turbo W16 8.0L thể hiện từng đường ống dẫn và logo Bugatti."
        ));

        seedCars.add(new Car(
            "Ferrari Monza SP2 Speedster", "MONZA SP2", 4100000L, 5100000L,
            "/images/cars/model-2.jpg", "MODEL_HIGHLIGHTS", "MODEL_CARS",
            "1:18 Collector Scale", "Hợp kim đúc nguyên khối sơn bóng công nghệ cao",
            "Thiết kế Speedster mui trần không kính chắn gió, 2 ghế ngồi bọc da nâu cognac thủ công",
            "Mẫu xe siêu hiếm trong bộ sưu tập Icona của hãng ngựa chồm. Từng nút bấm điều khiển trên táp-lô đều được thu nhỏ với tỉ lệ hoàn hảo."
        ));

        seedCars.add(new Car(
            "Porsche 911 GT3 R Neon Pink", "PS 911 GT3 RSR", 3650000L, 4550000L,
            "/images/cars/model-3.jpg", "MODEL_HIGHLIGHTS", "MODEL_CARS",
            "1:18 Collector Scale", "Hợp kim cao cấp phủ sơn dạ quang Neon nổi bật",
            "Trang bị lồng chống lật roll-cage chuẩn FIA, đai an toàn 6 điểm bằng vải thật",
            "Chiếc xe đua đường trường GT3 R rực rỡ dưới ánh đèn showroom, là điểm nhấn ấn tượng trong bất kỳ bộ sưu tập mô hình xe hơi nào."
        ));

        carRepository.saveAll(seedCars);
    }

    public List<Car> getAllCars(String section, String category, String search) {
        if (search != null && !search.trim().isEmpty()) {
            return carRepository.findByNameContainingIgnoreCase(search.trim());
        }
        if (section != null && !section.trim().isEmpty()) {
            return carRepository.findBySection(section.trim().toUpperCase());
        }
        if (category != null && !category.trim().isEmpty()) {
            return carRepository.findByCategory(category.trim().toUpperCase());
        }
        return carRepository.findAll();
    }

    public Optional<Car> getCarById(Long id) {
        return carRepository.findById(id);
    }

    public List<CategoryDto> getCategories() {
        List<CategoryDto> list = new ArrayList<>();
        list.add(new CategoryDto("RC_CARS", "RC Cars", "car", 12));
        list.add(new CategoryDto("MODEL_CARS", "Model Cars", "model", 18));
        list.add(new CategoryDto("BATTERIES", "Batteries", "battery", 8));
        list.add(new CategoryDto("CONTROLLERS", "Controllers", "controller", 6));
        list.add(new CategoryDto("OTHER_PARTS", "Other Parts", "parts", 24));
        return list;
    }

    public List<ReviewDto> getReviews() {
        List<ReviewDto> list = new ArrayList<>();
        list.add(new ReviewDto(
            1L, "eng******", 5,
            "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Duis semper lacus at tempor commodo. Phasellus posuere fermentum odio, vel aliquet ligula consequat accumsan.",
            "Bought 1:18 Pagani Huayra Model on July 8, 2026",
            "/images/cars/review-1.jpg"
        ));
        list.add(new ReviewDto(
            2L, "shi******", 5,
            "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Duis semper lacus at tempor commodo. Phasellus posuere fermentum odio, vel aliquet ligula consequat accumsan.",
            "Bought 1:18 Bugatti Chiron Model on Aug 15, 2026",
            "/images/cars/review-2.jpg"
        ));
        list.add(new ReviewDto(
            3L, "mel*****", 5,
            "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Duis semper lacus at tempor commodo. Phasellus posuere fermentum odio, vel aliquet ligula consequat accumsan.",
            "Bought 1:10 Nissan GT-R RC Model on Sept 2, 2026",
            "/images/cars/review-3.jpg"
        ));
        return list;
    }
}
