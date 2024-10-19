import React, { useState, useRef } from 'react';
import { useNavigate } from 'react-router-dom'; // useNavigate 사용

interface VideoCardProps {
    song: {
        videoId: string;
        title: string;
        vtuberName: string;
        publishedAt: string;
        viewCount: number;
        channelId: string; // channelId 속성 추가
    };
}

const VideoCard: React.FC<VideoCardProps> = ({ song }) => {
    const navigate = useNavigate();
    const [hovered, setHovered] = useState(false);
    const enterTimeoutRef = useRef<number | null>(null);
    const leaveTimeoutRef = useRef<number | null>(null);
    const thumbnailUrl = `https://img.youtube.com/vi/${song.videoId}/0.jpg`;

    const handleMouseEnter = () => {
        if (leaveTimeoutRef.current) {
            clearTimeout(leaveTimeoutRef.current); // 마우스가 들어가면 leave 타이머를 취소
        }
        enterTimeoutRef.current = window.setTimeout(() => {
            setHovered(true); // 0.25초 후 상세 정보 표시
        }, 250);
    };

    const handleMouseLeave = () => {
        if (enterTimeoutRef.current) {
            clearTimeout(enterTimeoutRef.current); // 마우스가 빠져나가면 enter 타이머를 취소
        }
        leaveTimeoutRef.current = window.setTimeout(() => {
            setHovered(false); // 0.25초 후 원래 상태로 돌아감
        }, 250);
    };

    // 제목 클릭 시 검색 페이지로 이동
    const handleTitleClick = (event: React.MouseEvent) => {
        event.stopPropagation(); // 부모 요소로의 이벤트 전파를 막음
        navigate(`/search?query=${encodeURIComponent(song.title)}`);
    };

    // 채널명 클릭 시 해당 채널의 노래 검색 페이지로 이동 (채널 ID 사용)
    const handleChannelClick = (event: React.MouseEvent) => {
        event.stopPropagation(); // 부모 요소로의 이벤트 전파를 막음
        navigate(`/search?channelId=${encodeURIComponent(song.channelId)}`);
    };

    // 비디오 카드 클릭 시 유튜브 링크로 이동
    const handleCardClick = () => {
        window.open(`https://www.youtube.com/watch?v=${song.videoId}`, '_blank'); // 새 탭에서 유튜브로 이동
    };

    return (
        <div
            className="video-card"
            onMouseEnter={handleMouseEnter}
            onMouseLeave={handleMouseLeave}
            onClick={handleCardClick} // 카드 클릭 시 유튜브 영상으로 이동
        >
            {!hovered ? (
                <div className="thumbnail-content">
                    <img src={thumbnailUrl} alt={song.title} className="thumbnail" />
                    <h3 onClick={handleTitleClick} style={{ cursor: 'pointer' }}>
                        {song.title}
                    </h3>
                </div>
            ) : (
                <div className="hover-content">
                    <h3 onClick={handleTitleClick} style={{ cursor: 'pointer' }}>
                        {song.title}
                    </h3>
                    <p onClick={handleChannelClick} style={{ cursor: 'pointer', color: 'blue' }}>
                        채널명: {song.vtuberName}
                    </p>
                    <p>조회수: {song.viewCount.toLocaleString()}</p>
                    <p>게시일: {new Date(song.publishedAt).toLocaleDateString()}</p>
                </div>
            )}
        </div>
    );
};

export default VideoCard;
