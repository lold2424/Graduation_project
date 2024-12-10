import React, { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import axios from 'axios';
import './VtuberDetailPage.css';

const apiUrl = process.env.REACT_APP_API_URL;

interface VtuberDetail {
    name: string;
    subscribers: number;
    gender: string | null;
    songCount: number;
    channelImg: string;
}

const VtuberDetailPage: React.FC = () => {
    const { channelId } = useParams<{ channelId: string }>();
    const [vtuberDetail, setVtuberDetail] = useState<VtuberDetail | null>(null);
    const navigate = useNavigate();

    useEffect(() => {
        if (channelId) {
            axios
                .get(`${apiUrl}/api/v1/vtubers/${channelId}/details`)
                .then((response) => {
                    setVtuberDetail(response.data);
                })
                .catch((error) => {
                    console.error('버튜버 상세 정보를 가져오는 중 오류 발생:', error);
                });
        }
    }, [channelId]);

    if (!vtuberDetail) {
        return <p>로딩 중...</p>;
    }

    const handleGoToYoutube = () => {
        window.open(`https://www.youtube.com/channel/${channelId}`, '_blank');
    };

    const genderText =
        vtuberDetail.gender === 'female'
            ? '여성'
            : vtuberDetail.gender === 'male'
                ? '남성'
                : '혼성';

    return (
        <div className="vtuber-detail-page">
            <div className="detail-container">
                <div className="profile-section">
                    <img
                        src={vtuberDetail.channelImg}
                        alt={vtuberDetail.name}
                        className="profile-image"
                    />
                </div>
                <div className="info-section">
                    <h1 className="vtuber-name">{vtuberDetail.name}</h1>
                    <p className="vtuber-info">구독자 수: {vtuberDetail.subscribers.toLocaleString()}</p>
                    <p className="vtuber-info">성별: {genderText}</p>
                    <p className="vtuber-info">등록된 노래 수: {vtuberDetail.songCount}</p>
                    <button onClick={handleGoToYoutube} className="action-button youtube-button">
                        유튜브로 이동
                    </button>
                    <button onClick={() => navigate(-1)} className="action-button back-button">
                        뒤로 가기
                    </button>
                </div>
            </div>
        </div>
    );
};

export default VtuberDetailPage;
