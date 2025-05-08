package com.server.util;

public class AvatarUtil {
    private static final String[] avatars =new String[]{
            "https://bpic.588ku.com/element_origin_min_pic/23/07/11/d32dabe266d10da8b21bd640a2e9b611.jpg!r650",
            "https://ts1.tc.mm.bing.net/th/id/R-C.987f582c510be58755c4933cda68d525?rik=C0D21hJDYvXosw&riu=http%3a%2f%2fimg.pconline.com.cn%2fimages%2fupload%2fupc%2ftx%2fwallpaper%2f1305%2f16%2fc4%2f20990657_1368686545122.jpg&ehk=netN2qzcCVS4ALUQfDOwxAwFcy41oxC%2b0xTFvOYy5ds%3d&risl=&pid=ImgRaw&r=0",
            "https://img.tukuppt.com/ad_preview/00/20/23/5c9a118f33ecf.jpg!/fw/780",
            "https://ts1.tc.mm.bing.net/th/id/R-C.6f18e58bf3bacbcee226cb76dfc089a3?rik=61%2bMLnhOzFiZOg&riu=http%3a%2f%2fimages.shejidaren.com%2fwp-content%2fuploads%2f2014%2f07%2f085628m6c.jpg&ehk=v1vDvzVgPRc3irw6n%2bs5gF%2b5SEc1uCstErDhRaxqlgE%3d&risl=&pid=ImgRaw&r=0",
            "https://tse1-mm.cn.bing.net/th/id/OIP-C.Ko3rLErQKPke4WpPgvdBBAHaHa?rs=1&pid=ImgDetMain",
            "https://ts1.tc.mm.bing.net/th/id/R-C.a8553f142638e741396e386b43c2bca7?rik=dzSGk6XRsxAAsQ&riu=http%3a%2f%2fseopic.699pic.com%2fphoto%2f50062%2f5890.jpg_wh1200.jpg&ehk=BgxiqxvzoNQd0pZHWV4VPOMbYgqM76WDMt8RDzCjoYY%3d&risl=&pid=ImgRaw&r=0",
            "https://img.shetu66.com/2023/07/04/1688453333865029.png",
            "https://ts1.tc.mm.bing.net/th/id/R-C.2663ebde921eda7ebf64851aa793d383?rik=EnjW84nVKXs8dw&riu=http%3a%2f%2fimages.shejidaren.com%2fwp-content%2fuploads%2f2014%2f07%2f085629Wuk.jpg&ehk=p41wmwz3WOn90Pa1f4GaIwkrTicLgLUGlX4QFUK7Dr0%3d&risl=&pid=ImgRaw&r=0",
            "https://pic1.zhimg.com/v2-d735e7d4e6f266eb697a00f29dccf97d_720w.jpg?source=172ae18b",
            "https://ts1.tc.mm.bing.net/th/id/R-C.b462f4f34a59260f49ec40a176468c0e?rik=b2hc1zyYFJYtLw&riu=http%3a%2f%2fseopic.699pic.com%2fphoto%2f50020%2f5325.jpg_wh1200.jpg&ehk=NSMaSPA3bpcWKSEcxGbAy%2fmr%2bAeOu5I1qgC6xAJIae8%3d&risl=&pid=ImgRaw&r=0"
    };

    public static String randomAvatar(){
        int randomInt = (int)(Math.random() * avatars.length);
        return avatars[randomInt];
    }
}
